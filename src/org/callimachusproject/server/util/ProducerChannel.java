/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.server.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A Piped {@link ReadableByteChannel} that runs the producer in another thread.
 * 
 * @author James Leigh
 * 
 */
public class ProducerChannel implements ReadableByteChannel {
	public interface WritableProducer {
		void produce(WritableByteChannel ch) throws IOException;
	}

	private static ExecutorService executor = ManagedExecutors.getProducerThreadPool();
	private final WritableProducer producer;
	private final SourceChannel ch;
	private final Future<Void> task;
	private final CountDownLatch latch = new CountDownLatch(1);
	private IOException io;
	private RuntimeException runtime;
	private Error error;

	public ProducerChannel(final WritableProducer producer) throws IOException {
		this.producer = producer;
		Pipe pipe = Pipe.open();
		this.ch = pipe.source();
		final SinkChannel sink = pipe.sink();
		task = executor.submit(new Runnable() {
			public void run() {
				try {
					producer.produce(sink);
				} catch (InterruptedIOException e) {
					// exit
				} catch (ClosedChannelException e) {
					// exit
				} catch (IOException e) {
					io = e;
				} catch (RuntimeException e) {
					runtime = e;
				} catch (Error e) {
					error = e;
				} finally {
					try {
						sink.close();
					} catch (InterruptedIOException e) {
						// exit
					} catch (ClosedChannelException e) {
						// exit
					} catch (IOException e) {
						io = io == null ? e : io;
					} catch (RuntimeException e) {
						runtime = runtime == null ? e : runtime;
					} catch (Error e) {
						error = error == null ? e : error;
					} finally {
						latch.countDown();
					}
				}
			}

			public String toString() {
				return producer.toString();
			}
		}, null);
	}

	public String toString() {
		return producer.toString();
	}

	public boolean isOpen() {
		return ch.isOpen();
	}

	public void close() throws IOException {
		try {
			verify();
			task.cancel(true);
		} finally {
			ch.close();
		}
		verify();
		try {
			latch.await();
		} catch (InterruptedException e) {
			InterruptedIOException exc;
			exc = new InterruptedIOException(e.toString());
			exc.initCause(e);
			throw exc;
		}
	}

	public int read(ByteBuffer dst) throws IOException {
		verify();
		return ch.read(dst);
	}

	private void verify() throws IOException {
		try {
			if (io != null)
				throw io;
		} finally {
			io = null;
		}
		try {
			if (runtime != null)
				throw runtime;
		} finally {
			runtime = null;
		}
		try {
			if (error != null)
				throw error;
		} finally {
			error = null;
		}
	}
}