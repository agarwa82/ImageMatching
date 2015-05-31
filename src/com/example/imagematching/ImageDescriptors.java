package com.example.imagematching;

import org.opencv.core.Mat;

public class ImageDescriptors {

   public Mat FeaturesUnclustered ;
	
   public Mat getFeaturesUnclustered() {
		return FeaturesUnclustered;
	}
	public void setFeaturesUnclustered(Mat featuresUnclustered) {
		FeaturesUnclustered = featuresUnclustered;
	}
	public String getImageName() {
		return imageName;
	}
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	private String imageName;
	private int score;
	
	
	public ImageDescriptors(){
		
		FeaturesUnclustered = new Mat();
		imageName = "";
		score = 0;
	}
	
}
