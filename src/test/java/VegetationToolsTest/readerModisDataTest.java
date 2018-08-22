package VegetationToolsTest;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.junit.Test;
import VegetationTools.readerModisData;

public class readerModisDataTest {

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

		readerModisData modisReader= new readerModisData();

		modisReader.inFolder="resources/Input/LaiMap";
		
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = "resources/Input/Centroid.shp";		
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;


		OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
		writer.file = "resources/Output/LAIfromCentroid.csv ";
		writer.tStart = reader.tStart;
		writer.tTimestep = reader.tTimestep;
		


		while( reader.doProcess ) {
			reader.nextRecord();
			modisReader.tCurrent=reader.tCurrent;
			modisReader.inStations = stationsFC;
			modisReader.fStationsid = "ID";
			modisReader.dataType=".tif";
			modisReader.scaleFactor=0.1;
			modisReader.prj="resources/Input/SystemRif.prj";

			modisReader.process();

			HashMap<Integer, double[]> resultD = modisReader.outValueHM;



			writer.inData = resultD;
			writer.writeNextLine();

		}
		//
		reader.close();
		writer.close();


	}


}
