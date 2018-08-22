package CentroidsGenerator;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
public class Centroid {


	@Description("Path to the input folder")
	@In
	public String inFolder;
	@Description("Path to the output folder")
	@In
	public String outputPath;
		
	@Description("Input data type: tif, tiff, asc")
	@In
	public String inputDataType;	
	
	@Description("data type: tif, tiff, asc")
	@In
	public String dataType;
	
	@Description("Prefix of the name of the output")
	@In
	public String nameOfOutput;
	
	@Description("Reference system")
	@In
	public String referenceSystem;
	
	
	LinkedHashMap<Integer, Coordinate> cellGrid;
	double rasterValue;
	WritableRaster rasterGrid;
	RegionMap regionMap;
	
	double defaultValue =-9999;
	double [] previousValue;
	double [] LAIarray;
	int step=0;
	double value;
	double longitude;
	double latitude;
	double elevation;
	
	double meanLongitude;
	double meanLatitude;
	double meanElevation;
	
	double countLongitude;
	double countLatitude;
	double countElevation;
	CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
	
	@Execute
	public void process() throws Exception {
		File currentDirectory = new File(inFolder);
		File[] filesList = currentDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(inputDataType);
				}		    
			});
		for(int iteratorRaster=0;iteratorRaster<filesList.length;iteratorRaster++){			 
			String name =filesList[iteratorRaster].toString();
			String[] pathToFile=name.split("/");
			System.out.println(pathToFile);
			System.out.println(pathToFile.length-1);
			String nameOfFile=pathToFile[pathToFile.length-1];
			String[] getTheID=nameOfFile.split("_");
			String theID=getTheID[getTheID.length-1];
			String[] theID2 = theID.split("\\.");
			System.out.println(theID2);
			String stationID = theID2[0];
			System.out.println(nameOfFile);
			System.out.println(2);
			OmsRasterReader map = new OmsRasterReader();
			map.file = name;
			map.fileNovalue = -9999.0;
			map.geodataNovalue = Double.NaN;
			map.process();
			GridCoverage2D mapGrid = map.outRaster;			
			
			WritableRaster rasterMap=mapsTransform(mapGrid);					
			GridGeometry2D mapGridGeo = mapGrid.getGridGeometry();
			cellGrid = getCoordinate(mapGridGeo);
			rasterGrid=mapsTransform(mapGrid);
			regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(mapGrid);
			int columns = regionMap.getCols();
			int rows = regionMap.getRows();
			double dx=regionMap.getXres()/2;			
			int k=0;
			countElevation = 0;
			countLongitude=0;
			countLatitude=0;
			int counterNonNullValue=0;
			for( int column = 0; column < columns; column++ ) {
				for( int row = rows-1; row >=0; row-- ) {						
					elevation 		= 	rasterMap.getSampleDouble(column, row, 0);
					if (elevation == (defaultValue)) {value = doubleNovalue;}
					else {
						Coordinate coordinate = (Coordinate) cellGrid.get(k);						
						counterNonNullValue++;									
						double longitude = coordinate.x+dx;
						double latitude = coordinate.y+dx;
						countLongitude = countLongitude + longitude;
						countLatitude = countLatitude+latitude;
						countElevation = countElevation+elevation;
						}
					k++;
					}
				}
			meanLongitude = countLongitude/counterNonNullValue;
			meanLatitude = countLatitude/counterNonNullValue;
			meanElevation =countElevation/counterNonNullValue;
			final SimpleFeatureType TYPE =
					DataUtilities.createType(
							"Location",
							//"the_geom:Point:srid=4326,"
							"the_geom:Point:srid="+referenceSystem+","
							+ // <- the geometry attribute: Point type
									"ID:String,"
							+ // <- a String attribute
									"Elevation:double" // a number attribute
							
							
							);
			System.out.println("TYPE:" + TYPE);		        
			
			String outputFile =outputPath+nameOfOutput+stationID;
			File file = new File(outputFile);		        
			List<SimpleFeature> features = new ArrayList<>();
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);			
		
			String ID = stationID;
			Point point = geometryFactory.createPoint(new Coordinate(meanLongitude, meanLatitude));
			featureBuilder.add(point);
			featureBuilder.add(ID);
			featureBuilder.add(meanElevation);
			SimpleFeature feature = featureBuilder.buildFeature(null);
			features.add(feature);	
			//String
			File newFile = getNewShapeFile(file);
			
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			Map<String, Serializable> params = new HashMap<>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);			
			ShapefileDataStore newDataStore =
					(ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
			/*
			                     * TYPE is used as a template to describe the file contents
			                     */
			newDataStore.createSchema(TYPE);                   
			/*			                     
			 * Write the features to the shapefile			
			 */
			Transaction transaction = new DefaultTransaction("create");
			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
			SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
			/*
			 * The Shapefile format has a couple limitations:	
			 * - "the_geom" is always first, and used for the geometry attribute name
			 * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
			 * - Attribute names are limited in length
			 * - Not all data types are supported (example Timestamp represented as Date)
			 *
			 * Each data store has different limitations so check the resulting SimpleFeatureType.
			 */
			System.out.println("SHAPE:" + SHAPE_TYPE);			
			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				/*
				 * SimpleFeatureStore has a method to add features from a
			     * SimpleFeatureCollection object, so we use the ListFeatureCollection
			     * class to wrap our list of features.
			     */
				SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
				featureStore.setTransaction(transaction);
				try {
					featureStore.addFeatures(collection);
					transaction.commit();
					} catch (Exception problem) {
						problem.printStackTrace();
						transaction.rollback();
						} finally {
							transaction.close();
							}
				//System.exit(0); // success!
				} else {
					System.out.println(typeName + " does not support read/write access");
					System.exit(1);
					}
			}
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
	
	public static File getNewShapeFile(File noExtenctionFile) {
		String path = noExtenctionFile.getAbsolutePath();
		String newPath = path+".shp";// path.substring(0, path.length() ) + ".shp";	
		File newFile = new File(newPath);		
		if (newFile.equals(noExtenctionFile)) {
			System.out.println("Error: cannot replace " + noExtenctionFile);
			System.exit(0);
			}		
		return newFile;
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
	
	private LinkedHashMap<Integer, Coordinate> getCoordinate(GridGeometry2D grid) {
		LinkedHashMap<Integer, Coordinate> out = new LinkedHashMap<Integer, Coordinate>();
		int count = 0;
		RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(grid);
		double cols = regionMap.getCols();
		double rows = regionMap.getRows();
		double south = regionMap.getSouth();
		double west = regionMap.getWest();
		double xres = regionMap.getXres();
		double yres = regionMap.getYres();
		double northing = south;
		double easting = west;
		for (int i = 0; i < cols; i++) {
			easting = easting + xres;
			for (int j = 0; j < rows; j++) {
				northing = northing + yres;
				Coordinate coordinate = new Coordinate();
				coordinate.x = west + i * xres;
				coordinate.y = south + j * yres;
				out.put(count, coordinate);
				count++;
			}
		}

		return out;
	}
}