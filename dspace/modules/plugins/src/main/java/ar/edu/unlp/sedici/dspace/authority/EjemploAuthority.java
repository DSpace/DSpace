package ar.edu.unlp.sedici.dspace.authority;

import java.util.ArrayList;
import java.util.Collection;

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

public class EjemploAuthority implements ChoiceAuthority {

    private static String keys[] = {
        "Acta_1", 
        "Acta_2",
        "Acta_3",
        "Acta_4",
        "Acta_5",
        "Acta_6",
        "Acta_7",
        "Acta_8",
        "Acta_9",
        "Acta_10",
        "Acta_11",
        "Acta_12",
        "Acta_13"    
    };

    private static String labels[] = {
        "Acta de Prueba numero Uno",
        "Acta de Prueba numero Dos",
        "Acta de Prueba numero Tres",
        "Acta de Prueba numero Cuatro",
        "Acta de Prueba numero Cinco",
        "Acta de Prueba numero Seis",
        "Acta de Prueba numero Siete",
        "Acta de Prueba numero Ocho",
        "Acta de Prueba numero Nueve",
        "Acta de Prueba numero Diez",
        "Acta de Prueba numero Once",
        "Acta de Prueba numero Doce",
        "Acta de Prueba numero Trece"        
    };

    public Choices getMatches(String field, String query, int collection, int start, int limit, String locale)
    {
        int longitud=0;
        Collection<Choice> resultado = new ArrayList<Choice>();
        for (int i = 0; i < labels.length; ++i){
        	if (labels[i].contains(query)){
              resultado.add(new Choice(keys[i], labels[i], labels[i]));
              longitud+=1;
        	}
        }
        
        Choice v[] = new Choice[longitud];
        int i=0;
        for (Choice choice : resultado) {
        	v[i]=choice;
        	i+=1;
		};

        return new Choices(v, 0, longitud, Choices.CF_ACCEPTED, false);
    }

    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
        /*for (int i = 0; i < values.length; ++i)
        {
            if (text.equalsIgnoreCase(values[i]))
            {
                Choice v[] = new Choice[1];
                v[0] = new Choice(String.valueOf(i), values[i], labels[i]);
                return new Choices(v, 0, v.length, Choices.CF_UNCERTAIN, false, 0);
            }
        }*/
        return new Choices(Choices.CF_NOTFOUND);
    }

    public String getLabel(String field, String key, String locale)
    {
        int inicio=0;
        boolean encontrado=false;
        int longitud=labels.length;
        String retorno= "No existe";
        String label;
        
        while ((inicio<longitud) && !encontrado){
        	label=keys[inicio];
        	if (label.equals(key)){
        		encontrado=true;
        		retorno=labels[inicio];
        	} else {
        		inicio+=1;
        	};
        }

        return retorno;

    }
}

