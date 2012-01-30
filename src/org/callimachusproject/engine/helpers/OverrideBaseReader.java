/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.engine.helpers;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.Base;
import org.callimachusproject.engine.events.Graph;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Subject;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.VarOrTermExpression;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.Reference;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.engine.model.VarOrIRI;
import org.callimachusproject.engine.model.VarOrTerm;

/**
 * Overrides the base and re-resolves all relative URIs.
 *  
 * @author James Leigh
 *
 */
public class OverrideBaseReader extends PipedRDFEventReader {
	private TermFactory tf = TermFactory.newInstance();
	private Base previously;
	private Base base;
	private boolean based;

	public OverrideBaseReader(Base from, Base to, RDFEventReader reader) {
		super(reader);
		this.previously = from;
		this.base = to;
	}

	@Override
	protected void process(RDFEvent event) throws RDFParseException {
		if (!based && base != null && !event.isStartDocument() && !event.isNamespace()) {
			add(base);
			based = true;
		}
		if (event.isBase()) {
			String docbase = event.asBase().getBase();
			if (previously == null || previously.resolve(docbase).equals(previously.getBase())) {
				if (base != null) {
					add(base);
				}
				based = true;
			} else {
				add(event);
				based = true;
			}
		} else if (event.isStartSubject() || event.isEndSubject()) {
			VarOrTerm term = event.asSubject().getSubject();
			VarOrTerm ref = relative(term);
			add(new Subject(event.isStart(), ref));
		} else if (event.isStartGraph() || event.isEndGraph()) {
			VarOrIRI term = event.asGraph().getGraph();
			VarOrIRI ref = relative(term);
			add(new Graph(event.isStart(), ref));
		} else if (event.isVarOrTerm()) {
			add(new VarOrTermExpression(relative(event.asVarOrTerm())));
		} else if (event.isTriple()) {
			Triple tp = event.asTriple();
			Node subj = relative(tp.getSubject());
			IRI pred = relative(tp.getPredicate());
			Term obj = relative(tp.getObject());
			add(new Triple(subj, pred, obj, tp.isInverse()));
		} else if (event.isTriplePattern()) {
			TriplePattern tp = event.asTriplePattern();
			VarOrTerm subj = relative(tp.getSubject());
			VarOrIRI pred = relative(tp.getPredicate());
			VarOrTerm obj = relative(tp.getObject());
			add(new TriplePattern(subj, pred, obj, tp.isInverse()));
		} else {
			add(event);
		}
	}

	private <T extends VarOrTerm> T relative(T term) {
		if (term.isReference())
			return (T) relative(term.asReference());
		return term;
	}

	private Reference relative(Reference term) {
		if (base == null)
			return term;
		String relative = term.asReference().getRelative();
		return tf.reference(base.resolve(relative), relative);
	}

}