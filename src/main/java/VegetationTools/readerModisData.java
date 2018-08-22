package VegetationTools;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.joda.time.DateTime;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class readerModisData {

	@Description("Input folder")
	@In
	public String inFolder;

	@Description("The path of the file containing the projection")
	@In
	public String prj;

	@Description("The current time.")
	@In
	public String tCurrent;

	@Description("data type: tif, tiff, asc")
	@In
	public String dataType;

	@Description("The scale factor.")
	@In
	public double scaleFactor;

	@Description("The shape file with the station measuremnts")
	@In
	public SimpleFeatureCollection inStations;

	@Description("The name of the field containing the ID of the station in the shape file")
	@In
	public String fStationsid;

	@Description(" The vetor containing the id of the station")
	Object []idStations;

	@Description("the linked HashMap with the coordinate of the stations")
	LinkedHashMap<Integer, Coordinate> stationCoordinates;

	@Description("List of the indeces of the columns of the station in the map")
	ArrayList <Integer> columnStation= new ArrayList <Integer>();

	@Description("List of the indeces of the rows of the station in the map")
	ArrayList <Integer> rowStation= new ArrayList <Integer>();

	@Description("The extracted LAI hashmap")
	@Out
	public HashMap<Integer, double[]> outValueHM;

	double RasterValue;
	double defaultValue =-9999;
	double [] previousValue;
	double [] LAIarray;
	int step=0;

	@Execute
	public void process() throws Exception {
		outValueHM = new HashMap<Integer, double[]>();		
		File curDir = new File(inFolder);
		File[] filesList = curDir.listFiles();
		Object [] newList=list(filesList);

		stationCoordinates = getCoordinate(inStations, fStationsid);
		// trasform the list of idStation into an array
		if (step==0) {		
			idStations= stationCoordinates.keySet().toArray();
			LAIarray=new double[idStations.length];
			previousValue=new double[idStations.length];
		}
		

		boolean flag=false;

		for(int iteratorRaster=0;iteratorRaster<newList.length;iteratorRaster++){
			//previousValue = LAIarray.clone(); 
			String name =newList[iteratorRaster].toString();
			String[] split1=name.split("/");
			String[] split=split1[split1.length-1].split("_");
			int year=Integer.parseInt(split[1].substring(1, 5));
			int day=Integer.parseInt(split[1].substring(5, 8));
			DateTime date = new DateTime().withDayOfYear(day).withYear(year);
			String date2=date.toString();
			date2=date2.substring(0, 10);
			String dateFile=date2+" 00:00";
			if (dateFile.equalsIgnoreCase(tCurrent)){
				flag=true;			
				String path=name.split("\\.")[0];		
				path=path+".prj";			
				File origin = new File(prj);			
				File f = new File(path);
				f.createNewFile();			
				FileUtils.copyFile(origin, f);

				OmsRasterReader map = new OmsRasterReader();
				map.file = name;
				map.fileNovalue = -9999.0;
				map.geodataNovalue = Double.NaN;
				map.process();
				GridCoverage2D mapGrid = map.outRaster;
				WritableRaster mapWR=mapsTransform(mapGrid);

				//  from pixel coordinates (in coverage image) to geographic coordinates (in coverage CRS)
				MathTransform transf = mapGrid.getGridGeometry().getCRSToGrid2D();

				// computing the reference system of the input DEM
				CoordinateReferenceSystem sourceCRS = mapGrid.getCoordinateReferenceSystem2D();

				//create the set of the coordinate of the station, so we can 
				//iterate over the set	
				Iterator<Integer> idIterator = stationCoordinates.keySet().iterator();

				for (int iteratorPoint=0;iteratorPoint<idStations.length;iteratorPoint++){
					// compute the coordinate of the station from the linked hashMap
					Coordinate coordinate = (Coordinate) stationCoordinates.get(idIterator.next());
					// define the position, according to the CRS, of the station in the map
					DirectPosition point = new DirectPosition2D(sourceCRS, coordinate.x, coordinate.y);

					// trasform the position in two the indices of row and column 
					DirectPosition gridPoint = transf.transform(point, null);
					
					// add the indices to a list
					columnStation.add((int) gridPoint.getCoordinate()[0]);
					rowStation.add((int) gridPoint.getCoordinate()[1]);

					LAIarray[iteratorPoint]=(mapWR.getSampleDouble(columnStation.get(iteratorPoint), rowStation.get(iteratorPoint), 0)>100)?defaultValue:
						mapWR.getSampleDouble(columnStation.get(iteratorPoint), rowStation.get(iteratorPoint), 0)*scaleFactor;
					previousValue[iteratorPoint] = LAIarray[iteratorPoint];
					} 
				}			
			else {
				if(flag==false){
					for (int iteratorPoint=0;iteratorPoint<idStations.length;iteratorPoint++){
						LAIarray[iteratorPoint]=previousValue[iteratorPoint];
						}
					}
				}
			
			}
		for (int iteratorPoint=0;iteratorPoint<idStations.length;iteratorPoint++){
			outValueHM.put((Integer)idStations[iteratorPoint], new double[]{LAIarray[iteratorPoint]});
			}
		step++;
		}
	public Object [] list(File[] filesList) throws Exception{
		Object [] list = null;
		ArrayList<String> arrayString = new ArrayList <String>();
		for(int iteratorRaster=0;iteratorRaster<filesList.length;iteratorRaster++){
			String name =filesList[iteratorRaster].getName();
			int split_length=name.split("\\.").length;
			String data_type=name.split("\\.")[split_length-1];
			if(data_type.equals(dataType)){
				arrayString.add(filesList[iteratorRaster].toString());
				}
			}
		list=arrayString.toArray();
		return list;
		}
	/**
	 * 	 * Gets the coordinate given the shp file and the field name in the shape with the coordinate of the station.
	 * 	 *
	 * 	 * @param collection is the shp file with the stations
	 * 	 * @param idField is the name of the field with the id of the stations
	 * 	 * @return the coordinate of each station
	 * 	 * @throws Exception the exception in a linked hash map
	 * 	 */
	private LinkedHashMap<Integer, Coordinate> getCoordinate(SimpleFeatureCollection collection, String idField)
			throws Exception {
		LinkedHashMap<Integer, Coordinate> id2CoordinatesMap = new LinkedHashMap<Integer, Coordinate>();
		FeatureIterator<SimpleFeature> iterator = collection.features();
		Coordinate coordinate = null;
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				int stationNumber = ((Number) feature.getAttribute(idField)).intValue();
				coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
				id2CoordinatesMap.put(stationNumber, coordinate);
				}
			} finally {
				iterator.close();
				}		
		return id2CoordinatesMap;
		}
	/**
	 * 	 * Maps reader transform the GrifCoverage2D in to the writable raster,
	 * 	 * replace the -9999.0 value with no value.
	 * 	 *
	 * 	 * @param inValues: the input map values
	 * 	 * @return the writable raster of the given map
	 * 	 */
	private WritableRaster mapsTransform ( GridCoverage2D inValues){		
		RenderedImage inValuesRenderedImage = inValues.getRenderedImage();
		WritableRaster inValuesWR = CoverageUtilities.replaceNovalue(inValuesRenderedImage, -9999.0);
		inValuesRenderedImage = null;
		return inValuesWR;
		}
	}