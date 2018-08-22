package VegetationTools;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.joda.time.DateTime;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class terrainCover {
	/*
	 * GNU GPL v3 License
	 *
	 * Copyright 2018 Michele Bottazzi
	 * 
	 * This program is free software: you can redistribute it and/or modify
	 * it under the terms of the GNU General Public License as published by
	 * the Free Software Foundation, either version 3 of the License, or
	 * (at your option) any later version.
	 *
	 * This program is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 * GNU General Public License for more details.
	 *
	 * You should have received a copy of the GNU General Public License
	 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
	 */
	@Description("Path to the input folder")
	@In
	public String inFolder;
	
	@Description("The current time.")
	@In
	public String tCurrent;
	
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
	
	@Description("The scale factor.")
	@In
	public double scaleFactor;
	
	@Description("The extracted mean LAI hashmap")
	@Out
	public HashMap<Integer, double[]> outValueMeanLai;
	public HashMap<Integer, double[]> outValueVegetatedArea;
		
	LinkedHashMap<Integer, Coordinate> cellGrid;
	WritableRaster rasterGrid;
	RegionMap regionMap;
	
	double defaultValue =255;
	
	int step=0;
	double rasterValue;

	double meanLai;
	double vegetatedArea;
	
	double previousVegetatedValue;
	double previousValue;
	
	int counterNullValue;
	int counterLaiValue;
	int counterUrbanValue;
	int counterWetlandValue;
	int counterSnowValue;
	int counterBareSoilValue;
	int counterWaterValue;
	
	double sumLai;
	int ID;
	 
	@Execute
	public void process() throws Exception {
		outValueMeanLai = new HashMap<Integer, double[]>();	
		outValueVegetatedArea = new HashMap<Integer, double[]>();		

		File currentDirectory = new File(inFolder);
		File[] filesList = currentDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(inputDataType);
				}		    
			});
		boolean flag=false;
		
		for(int iteratorRaster=0;iteratorRaster<filesList.length;iteratorRaster++){			 
			String name =filesList[iteratorRaster].toString();
			String[] pathToFile=name.split("/");
			System.out.println(pathToFile);
			String nameOfFile=pathToFile[pathToFile.length-1];
			String[] getTheID=nameOfFile.split("_");
			String theIDAndFormat=getTheID[getTheID.length-1];
			String[] theID = theIDAndFormat.split("\\.");
			String stationID = theID[0];
			ID = Integer.parseInt(stationID);
			
			// DATE
			int year=Integer.parseInt(getTheID[1].substring(1, 5));
			int day=Integer.parseInt(getTheID[1].substring(5, 8));
			DateTime date = new DateTime().withDayOfYear(day).withYear(year);
			String date2=date.toString();
			date2=date2.substring(0, 10);
			String dateFile=date2+" 00:00";
			
			if (dateFile.equalsIgnoreCase(tCurrent)){
		
				step++;
				System.out.println(step);
				OmsRasterReader map = new OmsRasterReader();
				map.file = name;
				map.fileNovalue = 255.0;
				map.geodataNovalue = 255.0;
				map.process();
				GridCoverage2D mapGrid = map.outRaster;	
				WritableRaster rasterMap=mapsTransform(mapGrid);					
				GridGeometry2D mapGridGeo = mapGrid.getGridGeometry();
				cellGrid = getCoordinate(mapGridGeo);
				rasterGrid=mapsTransform(mapGrid);
				regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(mapGrid);
				int columns = regionMap.getCols();
				int rows = regionMap.getRows();
				sumLai = 0;
				for( int column = 0; column < columns; column++ ) {
					for( int row = rows-1; row >=0; row-- ) {						
						rasterValue 		= 	rasterMap.getSampleDouble(column, row, 0);
						if (rasterValue > 0 && rasterValue <= 100) {		
							counterLaiValue++;									
							sumLai = sumLai+rasterValue;
							}
						else if (rasterValue == 250) {														 
							counterUrbanValue++;
						}
						else if (rasterValue == 251) {
							counterWetlandValue++;
						}
						else if (rasterValue == 252) {
							counterSnowValue++;						
						}
						else if (rasterValue == 253) {
							counterBareSoilValue++;
						}
						else if (rasterValue == 254) {
							counterWaterValue++;
						}
						else {
							counterNullValue++;
						}
					}
				}	
				double totalArea = counterLaiValue + counterUrbanValue + counterWetlandValue + counterSnowValue + 
						+ counterBareSoilValue + counterWaterValue;
				System.out.println(rows*columns);
				System.out.println("counterLaiValue " + counterLaiValue);
				System.out.println("counterNullValue " +totalArea);

				/*System.out.println(counterUrbanValue);
				System.out.println(counterWetlandValue);
				System.out.println(counterSnowValue);
				System.out.println(counterBareSoilValue);
				System.out.println(counterWaterValue);*/
				
				meanLai =(sumLai*scaleFactor)/counterLaiValue;
				vegetatedArea = counterLaiValue/(totalArea);
				previousValue = meanLai;
				previousVegetatedValue = vegetatedArea;
			}
			else {
				if(flag==false){
						meanLai=previousValue;
						vegetatedArea = previousVegetatedValue;
						}
				}
		}
					outValueMeanLai.put(ID, new double[]{meanLai});
					outValueVegetatedArea.put(ID, new double[]{vegetatedArea});
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
	 * 	 Maps reader transform the GrifCoverage2D in to the writable raster,
	 * 	 replace the -9999.0 value with no value.
	 * 	 @param inValues: the input map values
	 * 	 @return the writable raster of the given map
	 */
	private WritableRaster mapsTransform ( GridCoverage2D inValues){	
		RenderedImage inValuesRenderedImage = inValues.getRenderedImage();
		WritableRaster inValuesWR = CoverageUtilities.replaceNovalue(inValuesRenderedImage, -9999.0);
		inValuesRenderedImage = null;
		return inValuesWR;
		}
	static SimpleFeatureType createFeatureType(CoordinateReferenceSystem targetCRS) {		
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Location");
		builder.setCRS(targetCRS); // <- Coordinate reference system
		// add attributes in order
		builder.add("the_geom", Point.class);
        builder.length(15).add("ID", String.class); // <- 15 chars width for name field
        builder.add("Elevation", Float.class);

        // build the type
        final SimpleFeatureType LOCATION = builder.buildFeatureType();

        return LOCATION;
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