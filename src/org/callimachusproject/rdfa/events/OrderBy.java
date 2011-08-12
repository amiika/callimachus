/*
   Copyright (c) 2011 3 round Stones Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.rdfa.events;

import java.util.List;

import org.callimachusproject.rdfa.model.Var;

public class OrderBy extends RDFEvent {
	private List<Var> vars;

	public OrderBy(List<Var> vars) {
		assert vars != null && !vars.isEmpty();
		this.vars = vars;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ORDER BY");
		for (Var var : vars) {
			sb.append(" ");
			sb.append(var);
		}
		return sb.toString();
	}
}
