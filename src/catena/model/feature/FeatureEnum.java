package catena.model.feature;

public final class FeatureEnum {
	
	public static enum PairType { 
		event_event, event_timex, timex_timex; 
	}
	
	public static enum FeatureName {
		id,
		token, lemma, tokenSpace, lemmaSpace, tokenChunk,
		pos, mainpos, chunk, posCombined, mainposCombined, chunkCombined,
		ner, supersense, nerCombined, supersenseCombined,
		samePos, sameMainPos,
		entDistance, sentDistance, entOrder, timexInBetween,
		mainVerb, depRel, depPath, depOrder,
		modalVerb, hasModal,
		depTmxPath, depEvPath,
		eventClass, tense, aspect, tenseAspect, polarity,
		eventClassCombined, tenseCombined, aspectCombined, tenseAspectCombined, polarityCombined,
		sameEventClass, sameTense, sameAspect, sameTenseAspect, samePolarity,
		timexType, timexValue, dct, timexValueTemplate, timexTypeValueTemplate,
		tempMarker, causMarker, tempMarkerText, causMarkerText, tempMarkerClusText, causMarkerClusText,
		tempMarkerClusTextPos, causMarkerClusTextPos,
		tempMarkerTextSpace, causMarkerTextSpace, tempMarkerClusTextSpace, causMarkerClusTextSpace,
		tempMarkerPos, causMarkerPos, 
		tempMarkerDep1, tempMarkerDep2, tempMarkerDep1Dep2,
		causMarkerDep1, causMarkerDep2, causMarkerDep1Dep2,
		coref, wnSim, timexRule,
		tempMarkerTextPhrase, causMarkerTextPhrase,
		tempSignalClusText, tempSignalText, tempSignalPos, tempSignalDep1Dep2,
		tempSignal1ClusText, tempSignal1Text, tempSignal1Pos, tempSignal1Dep,
		tempSignal2ClusText, tempSignal2Text, tempSignal2Pos, tempSignal2Dep,
		causSignal1ClusText, causSignal1Text, causSignal1Pos, causSignal1Dep, 
		causSignal2ClusText, causSignal2Text, causSignal2Pos, causSignal2Dep,
		causSignalClusText, causSignalPos, causSignalDep1Dep2,
		causVerbClusText, causVerbPos,
		tempConnText, tempConnPos,
		label, labelCaus, 
		labelCollapsed,
		labelBinary,
		labelDense,
		labelCollapsed1, labelCollapsed2, labelCollapsed3,
		labelCollapsed4, labelCollapsed5, labelCollapsed6,
		labelCollapsed01, labelCollapsed02, labelCollapsed03;
	}

}
