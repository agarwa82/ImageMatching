package com.example.imagematching;

import java.util.Comparator;

public class ImageScore implements Comparable<ImageScore> {

	public ImageScore(){
		
	}
	String imageName; 
	int score;
	
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

	@Override
	public int compareTo(ImageScore another) {
		
		// TODO Auto-generated method stub
		return (another.score - this.score);
		
	}}
