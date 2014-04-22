package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy;

/**
 * @author terru
 * Esta clase mantiene la información para cada Item que procesa la tarea de curation
 * Cuando una regla encuentra algo para reportar,
 * debe utilizar esta clase para salvar el estado
 * Luego de cada item, el reporte se debe renovar
 */

public class Reporter {
	private String report;

	public String getReport() {
		return report;
	}
	
	public void addToItemReport(String Msg){
		this.report = this.report + Msg + "\n";
	}
	
}
