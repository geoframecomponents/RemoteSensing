package CentroidsGeneratorTest;

import org.junit.Test;
import CentroidsGenerator.Centroid;



public class CentroidTest {
	@Test
	public void test() throws Exception {
		Centroid centroids= new Centroid();
		centroids.inFolder="./resources/Input/Dem";		
		centroids.referenceSystem = "32632";
		centroids.inputDataType=".asc";
		centroids.outputPath ="./resources/Output/";
		centroids.nameOfOutput="Centroid";			
		centroids.process();		
		}		
	}
		

	

		
