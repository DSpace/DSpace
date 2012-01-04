/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.dspace.authority;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.authority.Choice;

import ar.edu.unlp.sedici.sedici2003.model.Personas;

public class SeDiCI2003Authors extends SeDiCI2003AuthorityProvider{
	
	@Override
	protected List<Choice> findSeDiCI2003Entities(String text, int start, int limit, ChoiceFactory choiceFactory) {
		
		String[] parts = text.split(", ",1);
		String apellido = parts[0];
		String nombre = (parts.length == 2)?parts[1]:"";
		List<Personas> personas = Personas.findPersonasesByApellidoYNombre(apellido, nombre, start, limit);
		List<Choice> choices= new ArrayList<Choice>(personas.size());
		for (Personas p : personas) {
			choices.add(choiceFactory.createChoice(p.getId(), p.getApellidoYNombre(), p.getApellidoYNombre()));
		}
		return choices;
	}

	protected String getSeDiCI2003EntityLabel(String key) {
		Personas p = Personas.findPersonas(Integer.valueOf(key));
		return p.getApellidoYNombre();
	}
}
