package org.dspace.app.webui.simulation;

import java.util.ArrayList;
import java.util.Random;

import org.dspace.app.webui.components.StatisticsBean;


/**
 * Simulates statistics data. <br>
 * This class <b>must not</b> be used in production. Make shure that
 * the parameter <code>dspace.stats.simulation.active</code> has <i>false</i> value
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class StatisticsSimulator 
{

	private static final String SIMULATED_TITLE = "Título de estatística simulada - Título de estatística simulada";
	
	/**
	 * Simulates visit stats
	 * @return Simulated object
	 */
	public static StatisticsBean simulateVisitStats()
	{
		return new StatisticsBean(){
			private static final long serialVersionUID = 1L;

		{
			/** Label das colunas **/
			setColLabels(new ArrayList<String>(){
				private static final long serialVersionUID = 1L;
			{
				add(SIMULATED_TITLE);
			}});
			
			setMatrix(new String[][]{{String.valueOf(new Random().nextInt(1000))}});
		}};
		
	}

	/**
	 * Simulate monthly visits
	 * @return Simulated object
	 */
	public static StatisticsBean simulateMonthlyVisits() 
	{
		return new StatisticsBean(){
			private static final long serialVersionUID = 1L;

		{
			/** Meses **/
			setColLabels(new ArrayList<String>(){
				private static final long serialVersionUID = 1L;
			{
				add("Fevereiro 2014");
				add("Março 2014");
				add("Abril 2014");
				add("Maio 2014");
				add("Junho 2014");
				add("Julho 2014");
				add("Agosto 2014");
			}});
			
			setMatrix(new String[][]{{
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000))
				
			}});
			
			setRowLabels(new ArrayList<String>(){
				private static final long serialVersionUID = 1L;
			{
				add(SIMULATED_TITLE);
			}});
		}};	
	}

	
	/**
	 * Simulate file download
	 * @return Simulated object
	 */
	public static StatisticsBean simulateFileDownloads() 
	{
		return new StatisticsBean(){
			private static final long serialVersionUID = 1L;

		{
			/** File names **/
			setColLabels(new ArrayList<String>(){
				private static final long serialVersionUID = 1L;
			{
				add("Arquivo 01.png");
				add("Arquivo 02.mp3");
				add("Arquivo 03.ppt");
				add("Arquivo 04.pdf");
			}});
			
			/** Amount of downloads **/
			setMatrix(new String[][]{{
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000))
			}});

		}};
		
	}

	/**
	 * Simulate city visits
	 * @return Simulated object
	 */
	public static StatisticsBean simulateCountryVisits() 
	{
		return new StatisticsBean(){
			private static final long serialVersionUID = 1L;

		{
			/** Meses **/
			setColLabels(new ArrayList<String>(){
				private static final long serialVersionUID = 1L;
			{
				add("Brasil");
				add("Alemanha");
				add("Inglaterra");
				add("Argentina");
				add("Portugal");
				add("China");
				add("Venezuela");
				add("EUA");
				add("Itália");
				add("Ucrânia");
			}});
			
			setMatrix(new String[][]{{
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000)),
				String.valueOf(new Random().nextInt(1000))
				
			}});
			
		}};		
	}
	
	/**
	 * Simulate city visits
	 * @return Simulated object
	 */
	public static StatisticsBean simulateCityVisits() 
	{
		return new StatisticsBean(){
			private static final long serialVersionUID = 1L;
			
			{
				/** Meses **/
				setColLabels(new ArrayList<String>(){
					private static final long serialVersionUID = 1L;
					{
						add("Brasília");
						add("Rio Grande");
						add("Beijing");
						add("Porto Alegre");
						add("Rio De Janeiro");
						add("São Carlos");
						add("Fortaleza");
						add("João Pessoa");
						add("Natal");
						add("Salvador");
					}});
				
				setMatrix(new String[][]{{
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000)),
					String.valueOf(new Random().nextInt(1000))
					
				}});
				
			}};		
	}
	
}
