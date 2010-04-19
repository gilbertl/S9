package com.gilbertl.s9;

import java.util.Comparator;

public class LetterFrequencyComp implements Comparator<Character> {

	public static final char [] FREQ_ORDER =
		{'e', 't', 'a', 'o', 'i', 'n', 's', 'h', 'r', 'd', 'l', 'c', 'u',
			'm', 'w', 'f', 'g', 'y', 'p', 'b', 'v', 'k', 'j', 'x', 'q', 'z'};
	
	public int compare(Character char1, Character char2) {
		char c1 = Character.toLowerCase(char1);
		char c2 = Character.toLowerCase(char2);
		int rank1 = -1, rank2 = -1;
		
		for (int i = 0; i < FREQ_ORDER.length; i++) {
			if (c1 == FREQ_ORDER[i]) {
				rank1 = i;
				break;
			}
		}
		
		for (int i = 0; i < FREQ_ORDER.length; i++) {
			if (c2 == FREQ_ORDER[i]) {
				rank2 = i;
				break;
			}
		}
		
		if (rank1 == -1) {
			// some symbol or number; make them last
			return 1;
		} else if (rank2 == -1) {
			return -1;
		} else {
			return rank1 - rank2;
		}
	}
}
