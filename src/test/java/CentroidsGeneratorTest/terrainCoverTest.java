package CentroidsGeneratorTest;

import java.util.HashMap;

import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.junit.Test;
import CentroidsGenerator.terrainCover;


public class terrainCoverTest {
	@Test
	public void test() throws Exception {	
		
		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file ="resources/Input/timeseries.csv";
		reader.idfield = "ID";
		reader.tStart = "2004-01-17 00:00";
		reader.tTimestep = 60;
		reader.tEnd = "2004-01-31 23:00";
		reader.fileNovalue = "-9999";

		reader.initProcess();
	
		terrainCover coverage= new terrainCover();
	
		coverage.inFolder="/home/drugo/eclipse-workspace/CentroidsGenerator/resources/Input/LaiMap_cut";
		//centroids.referenceSystem = CRS.decode("EPSG:31256");
		
		OmsTimeSeriesIteratorWriter writerLai = new OmsTimeSeriesIteratorWriter();
		writerLai.file = "resources/Output/meanLAI.csv ";
		writerLai.tStart = reader.tStart;
		writerLai.tTimestep = reader.tTimestep;
		
		OmsTimeSeriesIteratorWriter writerArea = new OmsTimeSeriesIteratorWriter();
		writerArea.file = "resources/Output/vegetatedArea.csv ";
		writerArea.tStart = reader.tStart;
		writerArea.tTimestep = reader.tTimestep;
		
		while( reader.doProcess ) {
			reader.nextRecord();
			
			coverage.tCurrent=reader.tCurrent;
			coverage.inputDataType="_4.tif";
			coverage.scaleFactor=0.1;
			coverage.outputPath ="/home/drugo/eclipse-workspace/CentroidsGenerator/resources/Output/LAI";
		
			coverage.process();
			
			HashMap<Integer, double[]> Lai = coverage.outValueMeanLai;
			writerLai.inData = Lai;
			writerLai.writeNextLine();
			
			HashMap<Integer, double[]> Area = coverage.outValueVegetatedArea;
			writerArea.inData = Area;
			writerArea.writeNextLine();

		}		
		reader.close();
		writerLai.close();
		writerArea.close();
	}
}