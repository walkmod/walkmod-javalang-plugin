/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
 Walkmod is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Walkmod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.visitors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.walkmod.exceptions.WalkModException;
import org.walkmod.javalang.visitors.VoidVisitorAdapter;
import org.walkmod.query.QueryEngine;
import org.walkmod.query.QueryEngineAware;
import org.walkmod.walkers.VisitorContext;

public abstract class VisitorSupport<A>  extends VoidVisitorAdapter<A> implements QueryEngineAware{

	private QueryEngine queryEngine;




	public void initialize(VisitorContext context, Object rootNode) {
		if (queryEngine == null) {
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("language", "groovy");
			List<String> includes = new LinkedList<String>();
			includes.add("query.alias.groovy");
			parameters.put("includes", includes);
			
			Object bean = context.getBean(
					"org.walkmod.query.ScriptingQueryEngine", parameters);
			if (bean != null) {
				if (bean instanceof QueryEngine) {
					queryEngine = (QueryEngine) bean;
				}

			} else {
				throw new WalkModException("Query Engine not found");
			}
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("node", rootNode);
		queryEngine.initialize(context, params);
	}

	

	public void setQueryEngine(QueryEngine queryEngine) {
		this.queryEngine = queryEngine;
	}

	public QueryEngine getQueryEngine() {
		return queryEngine;
	}
	
	public Object query(String query){
		if(queryEngine!=null){
			return queryEngine.resolve(query);
		}
		return null;
	}

	public Object query(Object context, String query){
		if(queryEngine != null){
			return queryEngine.resolve(context, query);
		}
		return null;
	}	

}
