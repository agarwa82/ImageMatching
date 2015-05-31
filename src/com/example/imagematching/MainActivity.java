package com.example.imagematching;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.example.imagematching.R;
import android.support.v7.app.ActionBarActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.provider.MediaStore;
import android.util.Log;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MainActivity extends ActionBarActivity {

	// Fields for Image capture and retrieval
	private static Uri selectedImage;
	private static InputStream imageStream;
	private static Bitmap yourSelectedImage, bmpimg;
	private ImageDescriptors descriptor;
	private ImageDescriptors descriptorCompare;
	private Mat image1;
	private Mat imgCompare;
	private MatOfKeyPoint keyPoints;
	private MatOfKeyPoint keyPoints2;
	private MainActivity cameraActivity = null;
	private static Uri imageUri = null;
	private static TextView imageDetails = null;
	private final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
	// private String filepathImage = "/storage/emulated/0/DCIM/ImageMatchingDatabase/";
	private String filepathImage = "/storage/emulated/0/ImageMatchingDatabase/";
	private Document doc;
	private boolean isWrite = false;
	private Element rootElement;
	private File descFile;
	private Mat queryDescriptor = new Mat();
	String matchFileName = "";
	File matchFile;

	public static String _imgpath; // Save the name of the picture taken
	private static String _descDir; // Save the Descriptor directory path
	private static String _outPath; // Save the output folder path
	private static boolean _matchModeFlag = true;
	private TextView tViewResponse;
	private static final String TAG = "Image Matching::Activity";

	String res;
	Long executionTime = 0L;
	RandomAccessFile raf;
	File reportfile;
	boolean success = false;
	String fileToRead;
	ImageDescriptors queryImageDescriptor = new ImageDescriptors();
	ImageDescriptors descriptorObject = new ImageDescriptors();
	ImageDescriptors matchingObjects = new ImageDescriptors();
	File descriptorFile;
	List<ImageScore> scoreRecords = new ArrayList<ImageScore>();
	private int countOfBestMatches = 0;
	private int currentLoc = 0;

	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
			Log.e(TAG, "Check failed for OpenCV initialisation");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tViewResponse = (TextView) findViewById(R.id.tViewImageCapture);
		cameraActivity = this;

		Log.i(TAG, "Trying to load OpenCV library");

		BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV Successfully loaded ");
				}
					break;

				default: {
					super.onManagerConnected(status);
				}
					break;
				}
			}
		};

		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}

		Button clickImage = (Button) findViewById(R.id.captureImage);

		/*
		 * another way of setting click listeners in Android 
		 * clickImage.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { // TODO Auto-generated method
		 * stub startCameraActivity();
		 * 
		 * } });
		 */

	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public int filterBestMatches(List<MatOfDMatch> origMatches) {

		Stack<DMatch> prunedMatches = new Stack<DMatch>();

		for (int i = 0; i < origMatches.size(); i++)

		{
			
			if (origMatches.get(i).toArray().length == 1
					|| origMatches.get(i).toArray()[0].distance
							/ origMatches.get(i).toArray()[1].distance < 0.6)
				prunedMatches.push(origMatches.get(i).toArray()[0]);


		}
		return prunedMatches.size();

	}

	public boolean matchImages(ImageDescriptors refDescriptor,
			ImageDescriptors descriptorToCompare, FileWriter bw, int i) {

		List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
		MatOfDMatch matchetTestObject = new MatOfDMatch();
		DMatch matcheTestObject2 = new DMatch();
		MatOfDMatch matchesFiltered = new MatOfDMatch();
		ImageScore score = new ImageScore();
		DescriptorMatcher matcher = DescriptorMatcher
				.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

		matcher.knnMatch(refDescriptor.getFeaturesUnclustered(),
				descriptorToCompare.getFeaturesUnclustered(), matches, 4);

		score.setImageName(refDescriptor.getImageName());
		score.setScore(filterBestMatches(matches));

		// adding first element to the list // Also checking whether list is not
		// empty for ArrayOutOfBoundException

		scoreRecords.add(score);
		System.out.println("Score values for " + score.getImageName() + ": "
				+ score.getScore());

		Collections.sort(scoreRecords);// Note>> used comparable , not
										// Comparator

		if (!scoreRecords.isEmpty()) {

			while (scoreRecords.size() > 4) {
				scoreRecords.remove(scoreRecords.size() - 1);

			}
		}

		// iterating over this 4 element list everytime to check whether four
		// suitable matches are obtained
		
		Iterator<ImageScore> ScoreListiter = scoreRecords.iterator();
		ImageScore scorePointer = new ImageScore();

		if (_matchModeFlag == true) { 
		while (ScoreListiter.hasNext()) {
				scorePointer = ScoreListiter.next();
				if (scorePointer.getScore() >=5)
					countOfBestMatches++; // found a competing match
				if (countOfBestMatches == 4)
					return true;
			}

		} 
		
		else if (_matchModeFlag == false && !scoreRecords.isEmpty() && scoreRecords.get(0).getScore() >=5)
			return true;

		Log.d("LOG!", "Number of good matches= " + matchesFiltered.size());

	
		return false;

	}

	public void writeIntoFile(FileWriter bw) {

		try {
			matchFileName = filepathImage + "AndroidAccuracy.txt";

			if (!scoreRecords.isEmpty()) {
				Iterator<ImageScore> iter = scoreRecords.iterator();
				do {
					/* writing scores to file from the calling method once all matches are
					 processed to give final list of scores of descending
					 order of scores .
					*/
					ImageScore scoreIter = iter.next();
					bw.write(scoreIter.getImageName() + "\n"); // appends the
																// string to the
																// file

				} while (iter.hasNext());
			}

			else
				
				bw.write("No Good Matches Found!"); // appends the string to the
													// file
		}

		catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void onRadioButtonClicked(View view) {
		
		// Is the button now checked?
		boolean checked = ((RadioButton) view).isChecked();

		// Check which radio button was clicked
		if (view.getId() == R.id.radio0) {
			if (checked)
				_matchModeFlag = true; // get similar match
		} else if (view.getId() == R.id.radio1) {
			if (checked)
				_matchModeFlag = false; // get exact match
		}

	}

	/** Called when the user clicks the button Take_Picture */
	public void takePictureActivity(View view) {
		startCameraActivity(view);
	}

	protected void startCameraActivity(View v) {

		Log.i("In camera activity ", "Entered Camera Activity");

		
		
		ContentValues values = new ContentValues();
		/*Not using timestamp in image name to be stored.
		 * String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());*/
		
		_imgpath = filepathImage + "query" + ".jpg";

		values.put(MediaStore.Images.Media.TITLE, _imgpath);
		values.put(MediaStore.Images.Media.DESCRIPTION,
				"Image capture by camera");

		Log.i("In Camera activity",
				"After Adding values to be presevered in image"
						+ values.toString());
		
		// imageUri is the current activity attribute, define and save it for
		// later usage
		
		imageUri = getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		
		// Create parameters for Intent with filename
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
				imageUri);
		takePictureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		// Checking return to camera activity
		takePictureIntent.putExtra("return-data", true);
		// Starting Camera Activity.
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(takePictureIntent, 1);
			Log.i(" In camera activity ",
					"End of startActivity & Activity Result");
		}

	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {

		Log.i(TAG, "Entered Activity Result after clickin picture");
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {

			if (resultCode == RESULT_OK) {

				/*********** Load Captured Image And Data Start ****************/

				Log.i(TAG, "Calling convert Image to File");
				String imageId = convertImageUriToFile(imageUri, cameraActivity);

			
			//  added for extracting features of clicked image
			
			
				// Create and execute AsyncTask to load capture image

				/*********** Load Captured Image And Data End ****************/
				// Add corresponding processed information here to show whether
				// blur / non-blur

				 new performMatchingProcess().execute("" + imageId);

				Toast.makeText(this, "Matching completed", Toast.LENGTH_SHORT)
						.show();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Program Broke Somewhere!",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Program Broke Somewhere!",
						Toast.LENGTH_SHORT).show();
			}
		}

	}

	private void readDescriptorFileAndMatch(Document docOfDescriptors,
			Mat queryDescriptor) {

		// TODO Auto-generated method stub
		// returns the matrix object of descriptors by reading xml of index i
		// moved to prev method
		// Mat candidateDescriptor = readMat("opencv_storage",
		// docOfDescriptors);
		// descriptorObject.setFeaturesUnclustered(candidateDescriptor);
		// matchImages(descriptorObject, queryDescriptor);

	}

	// To open xml file and read each descriptor file one by one.

	// read only
	public void open(String filePath, int index) {

		try {

			File xmlFile = new File(filePath + "image" + Integer.toString(index)
					+ ".xml");
			String imagName = filePath + "image" + Integer.toString(index)
					+ "jpg";
			descriptorObject.setImageName(imagName);

			if (xmlFile == null || xmlFile.isFile() == false) {
				System.err.println("Can not open file: " + filePath);

			} else {

				isWrite = false;
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(xmlFile);
				doc.getDocumentElement().normalize();

			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	/************ Convert Image Uri path to physical path **************/

	public static String convertImageUriToFile(Uri imageUri, Activity activity) {

		Cursor cursor = null;
		int imageID = 0;

		try {

			/*********** Which columns values want to get *******/
			String[] proj = { MediaStore.Images.Media.DATA,
					MediaStore.Images.Media._ID,
					MediaStore.Images.Thumbnails._ID,
					MediaStore.Images.ImageColumns.ORIENTATION };

			cursor = activity.managedQuery(

			imageUri, // Get data for specific image URI
					proj, // Which columns to return
					null, // WHERE clause; which rows to return (all rows)
					null, // WHERE clause selection arguments (none)
					null // Order-by clause (ascending by name)

					);

			// Get Query Data

			int columnIndex = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
			int columnIndexThumb = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID);
			int file_ColumnIndex = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

			// int orientation_ColumnIndex = cursor.
			// getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);

			int size = cursor.getCount();

			/******* If size is 0, there are no images on the SD Card. *****/

			if (size == 0) {

				imageDetails.setText("No Image");
			} else {

				int thumbID = 0;
				if (cursor.moveToFirst()) {

					/**************** Captured image details ************/

					/***** Used to show image on view in LoadImagesFromSDCard class ******/
					imageID = cursor.getInt(columnIndex);

					thumbID = cursor.getInt(columnIndexThumb);

					String Path = cursor.getString(file_ColumnIndex);

					// String orientation =
					// cursor.getString(orientation_ColumnIndex);

					String CapturedImageDetails = " CapturedImageDetails : \n\n"
							+ " ImageID :"
							+ imageID
							+ "\n"
							+ " ThumbID :"
							+ thumbID + "\n" + " Path :" + Path + "\n";

					// Show Captured Image detail on activity
					// Skipping this -not required to display image details
					// imageDetails.setText( CapturedImageDetails );

				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// Return Captured Image ImageID ( By this ImageID Image will load from
		// sdcard )

		return "" + imageID;
	}

	// Class with extends AsyncTask class

	public class performMatchingProcess extends AsyncTask<String, Void, String> {

		private ProgressDialog Dialog = new ProgressDialog(MainActivity.this);
		TextView matchingResults = (TextView) findViewById(R.id.matchingImages);

		Bitmap mBitmap;

		protected void onPreExecute() {
			/****** NOTE: You can call UI Element here. *****/

			// Progress Dialog
			Dialog.setMessage(" Loading image from Sdcard..");
			Dialog.show();
		}

		// Call after onPreExecute method
		protected String doInBackground(String... urls) {

			Bitmap bitmap = null;
			Bitmap newBitmap = null;
			Uri uri = null;
			String matchingResultString = "";

			try {

				/**
				 * Uri.withAppendedPath Method Description Parameters baseUri
				 * Uri to append path segment to pathSegment encoded path
				 * segment to append Returns a new Uri based on baseUri with the
				 * given segment appended to the path
				 */

				uri = Uri.withAppendedPath(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ""
								+ urls[0]);

				/************** Decode an input stream into a bitmap. *********/
				bitmap = BitmapFactory.decodeStream(getContentResolver()
						.openInputStream(uri));
				// adding to check converted image
				newBitmap = Bitmap.createScaledBitmap(bitmap, 2592, 1944, true);

				if (bitmap != null) {

					/********* Creates a new bitmap, scaled from an existing bitmap. ***********/

					if (newBitmap != null) {

						Mat imgToCompare = new Mat();
						Utils.bitmapToMat(newBitmap, imgToCompare);

						Mat dist_Image = new Mat(imgToCompare.rows(),
								imgToCompare.cols(), CvType.CV_32F);
						imgToCompare.convertTo(dist_Image, CvType.CV_32F);

						FeatureDetector fd = FeatureDetector
								.create(FeatureDetector.ORB);

						keyPoints = new MatOfKeyPoint();

						fd.detect(imgToCompare, keyPoints);

						DescriptorExtractor extractor = DescriptorExtractor
								.create(DescriptorExtractor.ORB);

						extractor.compute(imgToCompare, keyPoints,
								queryDescriptor);

						matchFileName = filepathImage + "AndroidAccuracy.txt";

						queryImageDescriptor
								.setFeaturesUnclustered(queryDescriptor);
						queryImageDescriptor.setImageName("query.jpg");

						FileWriter bw = new FileWriter(matchFileName, false);
						bw.flush();
						boolean foundMaxMatches = false;
						
					/* This method is used aprior.. to extract features of all images in given database and keep them 
					 * in respective xml files.  
					 * 
					 * 	for(int i=1; i< 101;i++)
					 {
							extractFeatures(filepathImage, i);
					 }*/
						
						List<Mat> refDescriptors = new ArrayList<Mat>();
						for (int i = 1; i < 102; i++) {

							open(filepathImage, i);

							
							  Log.i("Reading descriptors now :: for image ",
							  descriptorObject.getImageName());
							 

							Mat candidateDescriptor = readMat("opencv_storage",
									doc);
							descriptorObject
									.setFeaturesUnclustered(candidateDescriptor);
							descriptorObject.setImageName(filepathImage + "image"
									+ Integer.toString(i) + ".jpg");
							refDescriptors.add(candidateDescriptor);
							if (!foundMaxMatches){
								foundMaxMatches = matchImages(descriptorObject,
										queryImageDescriptor, bw,i);
								System.out.println("filepathImage"+ "image" + Integer.toString(i) +foundMaxMatches);
							}
							else {
								i = 102;
							}
						}

						Log.i("Finished matching. now writing",
								scoreRecords.toString());

						writeIntoFile(bw);
						bw.close();

						matchingResultString = displayMatches();
					}
				}
			} catch (IOException e) {
				
				// Error fetching image, try to recover

				/********* Cancel execution of this task. **********/
				cancel(true);
			}

			return matchingResultString;
		}

		protected void onPostExecute(String result) {

			// NOTE: You can call UI Element here.

			// Close progress dialog
			Dialog.dismiss();
			matchingResults.setText(result);

		}
	}

	public void extractFeatures(String filepath, int index) {

		descriptor = new ImageDescriptors();
		String indexAppend = Integer.toString(index);

		image1 = new Mat();
		String imageName = filepath + "image" + index + ".jpg";

		image1 = Highgui.imread(imageName);
		
		// Added resize code.
		Imgproc.cvtColor(image1, image1, Imgproc.COLOR_RGB2GRAY);
		Size dSize = new Size(640, 480);
		Imgproc.resize(image1, image1, dSize);

		Mat dist_Image = new Mat(image1.rows(), image1.cols(), CvType.CV_32F);
		image1.convertTo(dist_Image, CvType.CV_32F);

		FeatureDetector fd = FeatureDetector.create(FeatureDetector.ORB);

		DescriptorExtractor extractor = DescriptorExtractor
				.create(DescriptorExtractor.ORB);

		keyPoints = new MatOfKeyPoint();
		fd.detect(image1, keyPoints);
		Mat localDescriptor = new Mat();
		extractor.compute(image1, keyPoints, localDescriptor);

		System.out.println("keyPoints.size() : " + keyPoints.size());

		System.out.println("descriptors.size() : " + localDescriptor.size());

		descriptor.setFeaturesUnclustered(localDescriptor);
		descriptor.setImageName(imageName);

		System.out.println("Decriptor object"
				+ descriptor.getFeaturesUnclustered());
		System.out.println("Local desciptor" + localDescriptor.toString());

		// create descriptor file
		create(filepath + "image" + Integer.toString(index) + ".xml",
				localDescriptor);

	}

	// start writing descriptor matrix into xml file one by one 
	public void create(String filePath, Mat localDescriptor) {
		try {

			descFile = new File(filePath);
			// creating each file.
			// open(descFile);

			if (descFile == null) {
				System.err.println("Can not wrtie file: " + filePath);

			} else {

				isWrite = true;
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.newDocument();
				rootElement = doc.createElement("opencv_storage");
				doc.appendChild(rootElement);
				writeMat(localDescriptor, filePath, doc);

			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	//

	public void writeMat(Mat mat, String filePath, Document doc) {

		try {

			if (isWrite == false) {
				System.err.println("Try write to file with no write flags");
				return;
			}

			Element matrix = doc.createElement("descriptors");

			matrix.setAttribute("type_id", "opencv-matrix");
			rootElement.appendChild(matrix);

			Element rows = doc.createElement("rows");
			rows.appendChild(doc.createTextNode(String.valueOf(mat.rows())));

			Element cols = doc.createElement("cols");
			cols.appendChild(doc.createTextNode(String.valueOf(mat.cols())));

			Element dt = doc.createElement("dt");
			String dtStr;
			int type = mat.type();
			if (type == CvType.CV_32F) { // type == CvType.CV_32FC1
				dtStr = "f";
			} else if (type == CvType.CV_32S) { // type == CvType.CV_32SC1
				dtStr = "i";
			} else if (type == CvType.CV_16S) { // type == CvType.CV_16SC1
				dtStr = "s";
			} else if (type == CvType.CV_8U) { // type == CvType.CV_8UC1
				dtStr = "b";
			} else {
				dtStr = "unknown";
			}
			dt.appendChild(doc.createTextNode(dtStr));

			Element data = doc.createElement("data");
			String dataStr = dataStringBuilder(mat);
			data.appendChild(doc.createTextNode(dataStr));

			// append all to matrix
			matrix.appendChild(rows);
			matrix.appendChild(cols);
			matrix.appendChild(dt);
			matrix.appendChild(data);
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			DOMSource source = new DOMSource(doc);

			// Writing the elements to xml file thru StreamResult.

			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String dataStringBuilder(Mat mat) {
		StringBuilder sb = new StringBuilder();
		int rows = mat.rows();
		int cols = mat.cols();
		int type = mat.type();

		if (type == CvType.CV_32F) {
			float fs[] = new float[1];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					mat.get(r, c, fs);
					sb.append(String.valueOf(fs[0]));
					sb.append(' ');
				}
				sb.append('\n');
			}
		} else if (type == CvType.CV_32S) {
			int is[] = new int[1];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					mat.get(r, c, is);
					sb.append(String.valueOf(is[0]));
					sb.append(' ');
				}
				sb.append('\n');
			}
		} else if (type == CvType.CV_16S) {
			short ss[] = new short[1];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					mat.get(r, c, ss);
					sb.append(String.valueOf(ss[0]));
					sb.append(' ');
				}
				sb.append('\n');
			}
		} else if (type == CvType.CV_8U) {
			byte bs[] = new byte[1];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					mat.get(r, c, bs);
					sb.append(String.valueOf(bs[0]));
					sb.append(' ');
				}
				sb.append('\n');
			}
		} else {
			sb.append("unknown type\n");
		}

		return sb.toString();
	}

	// Inputs - Doc value and parent tag = opencv_storage

	public Mat readMat(String tag, Document doc) {
		if (isWrite) {
			System.err.println("Try read from file with write flags");
			return null;
		}

		NodeList nodelist = doc.getElementsByTagName(tag);
		Mat readMat = null;

		for (int i = 0; i < nodelist.getLength(); i++) {
			Node node = nodelist.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;

				String type_id = element.getAttribute("type_id");
				if ("opencv-matrix".equals(type_id) == false) {
					System.out.println("Fault type_id ");
				}

				String rowsStr = element.getElementsByTagName("rows").item(0)
						.getTextContent();
				String colsStr = element.getElementsByTagName("cols").item(0)
						.getTextContent();
				String dtStr = element.getElementsByTagName("dt").item(0)
						.getTextContent();
				String dataStr = element.getElementsByTagName("data").item(0)
						.getTextContent();

				int rows = Integer.parseInt(rowsStr);
				int cols = Integer.parseInt(colsStr);
				int type = CvType.CV_8U;

				Scanner s = new Scanner(dataStr);

				if ("f".equals(dtStr)) {
					type = CvType.CV_32F;
					readMat = new Mat(rows, cols, type);
					float fs[] = new float[1];
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							if (s.hasNextFloat()) {
								fs[0] = s.nextFloat();
							} else {
								fs[0] = 0;
								System.err
										.println("Unmatched number of float value at rows="
												+ r + " cols=" + c);
							}
							readMat.put(r, c, fs);
						}
					}
				} else if ("i".equals(dtStr)) {
					type = CvType.CV_32S;
					readMat = new Mat(rows, cols, type);
					int is[] = new int[1];
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							if (s.hasNextInt()) {
								is[0] = s.nextInt();
							} else {
								is[0] = 0;
								System.err
										.println("Unmatched number of int value at rows="
												+ r + " cols=" + c);
							}
							readMat.put(r, c, is);
						}
					}
				} else if ("s".equals(dtStr)) {
					type = CvType.CV_16S;
					readMat = new Mat(rows, cols, type);
					short ss[] = new short[1];
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							if (s.hasNextShort()) {
								ss[0] = s.nextShort();
							} else {
								ss[0] = 0;
								System.err
										.println("Unmatched number of int value at rows="
												+ r + " cols=" + c);
							}
							readMat.put(r, c, ss);
						}
					}
				} else if ("b".equals(dtStr)) {
					readMat = new Mat(rows, cols, type);
					byte bs[] = new byte[1];
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							if (s.hasNextByte()) {
								bs[0] = s.nextByte();
							} else {
								bs[0] = 0;
								System.err
										.println("Unmatched number of byte value at rows="
												+ r + " cols=" + c);
							}
							readMat.put(r, c, bs);
						}
					}
				}
			}
		}
		return readMat;
	}

	public void release() {
		try {
			if (isWrite == false) {
				System.err.println("Try release of file with no write flags");
				return;
			}

			DOMSource source = new DOMSource(doc);

			StreamResult result = new StreamResult(descFile);

			// write to xml file
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// do it
			transformer.transform(source, result);

		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Display the results
	protected String displayMatches() {

		// Location of the result file
		String _resFile = matchFileName;
		String result_String = new String();
		
		try {
			// Read the result file and its contents
			Scanner fileIn = new Scanner(new File(_resFile));
			String match1 = "";
			String match2 = "";
			String match3 = "";
			String match4 = "";
			if (fileIn.hasNext()) {
				match1 = fileIn.next();
			}
			if (fileIn.hasNext()) {
				match2 = fileIn.next();
			}
			if (fileIn.hasNext()) {
				match3 = fileIn.next();
			}
			if (fileIn.hasNext()) {
				match4 = fileIn.next();
			}

			// Display number of matches according to the selected mode
			if (_matchModeFlag == false) {
				result_String = match1;
			} else if (_matchModeFlag == true) {
				result_String = match1 + "\n" + match2 + "\n" + match3 + "\n"
						+ match4;
			}

		} catch (IOException ex) {

		}

		return result_String;
	}

}
