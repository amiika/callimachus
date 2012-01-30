/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.server.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;

/**
 * Reads an byte[] from an {@link ReadableByteChannel}.
 */
public class ByteArrayMessageWriter implements MessageBodyWriter<byte[]> {

	public boolean isText(MessageType mtype) {
		return false;
	}

	public long getSize(MessageType mtype, byte[] result, Charset charset) {
		return result.length;
	}

	public boolean isWriteable(MessageType mtype) {
		String mimeType = mtype.getMimeType();
		Class<?> type = mtype.clas();
		if (!type.isArray() || !Byte.TYPE.equals(type.getComponentType()))
			return false;
		return mimeType == null || !mimeType.contains("*")
				|| mimeType.startsWith("*")
				|| mimeType.startsWith("application/*");
	}

	public String getContentType(MessageType mtype, Charset charset) {
		String mimeType = mtype.getMimeType();
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*"))
			return "application/octet-stream";
		return mimeType;
	}

	public ReadableByteChannel write(MessageType mtype, byte[] result,
			String base, Charset charset) throws IOException {
		return ChannelUtil.newChannel(result);
	}

	public void writeTo(MessageType mtype, byte[] result, String base,
			Charset charset, OutputStream out, int bufSize) throws IOException {
		out.write(result);
	}
}