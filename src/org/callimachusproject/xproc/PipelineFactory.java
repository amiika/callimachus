package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xml.sax.SAXException;

public class PipelineFactory {

	public static PipelineFactory newInstance() {
		return new PipelineFactory();
	}

	private PipelineFactory() {
		super();
	}

	public Pipeline createPipeline(String systemId) {
		return new Pipeline(systemId);
	}

	public Pipeline createPipeline(InputStream in, String systemId)
			throws SAXException, IOException {
		return new Pipeline(in, systemId);
	}

	public Pipeline createPipeline(Reader reader, String systemId)
			throws SAXException, IOException {
		return new Pipeline(reader, systemId);
	}

}
