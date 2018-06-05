package CentroidsGeneratorTest;

import org.junit.Test;
import CentroidsGenerator.Centroid;



public class CentroidTest {
	@Test
	public void test() throws Exception {		
	

		Centroid centroids= new Centroid();
	

		centroids.inFolder="/home/drugo/eclipse-workspace/CentroidsGenerator/resources/Input/";// /Users/marialaura/Dropbox/dati_NewAge/EsercitazioniIdrologia2017/data/Basilicata/lai";
		
		//	centroids.nameOfOutput = "Centroid";	
			centroids.referenceSystem = "32632";
			centroids.inputDataType=".asc";
			centroids.outputPath ="/home/drugo/eclipse-workspace/CentroidsGenerator/resources/Output/";
			centroids.nameOfOutput="Centroid";
			
			centroids.process();

			

		}
		
		}
		

	

		
