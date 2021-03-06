package com.ls3.oie.temporalrules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import com.ls3.oie.clause.ClausIE;
import com.ls3.oie.graph.graphUtil;
import com.ls3.oie.parsing.possessiveComponents;
import com.ls3.oie.temporalmodel.DataWriter;
import com.ls3.oie.temporalmodel.KeyWord;
import com.ls3.oie.temporalmodel.KeyWordProposition;
import com.ls3.oie.temporalmodel.KeyWordPropositionOIE5;
import com.ls3.oie.temporalmodel.Sentence;
import com.ls3.oie.temporalmodel.extractedProposition;
import com.ls3.oie.temporalmodel.extractedRelationNodes;
import com.ls3.oie.temporalmodel.extractedRelationNodesOIE5;
import com.ls3.oie.temporalmodel.oie5Sentence;
import com.ls3.oie.temporalmodel.oieSentence;
import com.ls3.oie.temporalmodel.relationNodes;
import com.ls3.oie.tools.WildCardFileFilter;
import com.ls3.oie.tools.txtNormalization;

import de.l3s.boilerpipe.extractors.ArticleExtractor;


public class mainProcessCausalIE5 {
	public static List<String> tenseRanking(){
		List<String> tenseRankingList=new ArrayList<>();
		tenseRankingList.add("PAST-1");
		tenseRankingList.add("PAST_PERFECT-1");
		//tenseRankingList.add("PAST_CONTINUOUS-1");
		tenseRankingList.add("PRESENT-2");
		//tenseRankingList.add("PRESENT_CONTINUOUS-2");
		tenseRankingList.add("PRESENT_PERFECT-2");
		tenseRankingList.add("FUTURE-3");
		//tenseRankingList.add("FUTURE_PERFECT-3");
		return tenseRankingList;
	}
	
	public static List<Sentence> SentenceList(String inputFileName) throws IOException{
		List<Sentence> pSentenceList = new ArrayList<Sentence>();
		DataWriter 		Writer      = new DataWriter();
		pSentenceList = Writer.writeSentenceList(inputFileName); 
		return pSentenceList;
	}
	
	public static List<oieSentence> oieSentenceList(List<Sentence> pSentenceList){
		List<oieSentence> pOieSentenceList=new ArrayList<>();
		for (int i = 1; i < pSentenceList.size(); i++) { // Starting from sentence 1
			System.out.println(i);
			Sentence tempSentence=pSentenceList.get(i);
			System.out.println(tempSentence.sentenceContent());
			oieSentence tempOieSentence=new oieSentence();
			tempOieSentence.setSentence(tempSentence);
			tempOieSentence.loadclausePattern();
			tempOieSentence.loadExtractedProposition();
			pOieSentenceList.add(tempOieSentence);
		}
		return pOieSentenceList;
	}
	
	public static List<oie5Sentence> oie5SentenceList(List<Sentence> pSentenceList, String fileName) throws IOException{
		List<oie5Sentence> oie5SentenceList=new ArrayList<>();
		for (int i = 0; i < pSentenceList.size(); i++) {
			Sentence tempSentence=pSentenceList.get(i);
			System.out.println(tempSentence.sentenceContent());
			oie5Sentence tempOIE5Sen=new oie5Sentence();
			tempOIE5Sen.setSentence(tempSentence);
			//tempOIE5Sen.loadPattern_test();
			tempOIE5Sen.loadCausalPatternOIE5(fileName, i);
			tempOIE5Sen.loadExtractedPropositionOIE5();
			oie5SentenceList.add(tempOIE5Sen);
		}
		return oie5SentenceList;
	}
	
	public static List<String> processResult(List<Sentence> pSentenceList, List<String> pAllResorceRecord, String pColID) throws IOException{
		
		List<String> outputRecord=new ArrayList<>();
		
		List<String> tenseRankingList=tenseRanking();
		//List<String> timeRankingList=timeRanking();
		
		//List<Sentence> sentenceList = SentenceList(pSentenceList) ;
		List<Sentence> sentenceList = pSentenceList ;
		List<oie5Sentence> oie5SentenceList=oie5SentenceList(sentenceList, pColID);
		
		// Load resource and causalresource
		relationNodes initialNode=new relationNodes();
		List<relationNodes> relResourceList=initialNode.loadSource(pAllResorceRecord, pColID);
		List<relationNodes> relCausalResourceList=initialNode.loadCausalSource(pAllResorceRecord, pColID);
		List<relationNodes> relTempResourceList=initialNode.loadTemporalSource(pAllResorceRecord, pColID);
		
		// start here	
		extractedRelationNodesOIE5 _relationNodes=new extractedRelationNodesOIE5();  // Can thay doi vi tri nay 17 March 2013
		_relationNodes.setpOIESentence(oie5SentenceList);
		_relationNodes.loadRelationNodeList(pAllResorceRecord, pColID);
		
		// for updated relation list
		List<relationNodes> relOriginalList=_relationNodes.getRelNodesList();
		List<relationNodes> updateRelList=new ArrayList<>();
		
		// Can bo sung temporal standard resource (quan trong can phai thay doi)---------- 5 March 2019
		updateRelList=_relationNodes.updateRelationNodeListByFoundResource(relResourceList, relOriginalList);
		_relationNodes.addRelList(updateRelList);
		
		// tmx0 list
		Sentence sen0=sentenceList.get(0);
		KeyWord keywordTMX0=sen0.sentence.get(0);
		List<KeyWordPropositionOIE5> keyWordEventsList= new ArrayList<>();
		keyWordEventsList=oie5SentenceList.get(0).loadAllKeyWordPropositionByOIESentences(oie5SentenceList);
		List<relationNodes> tmx0List=_relationNodes.loadTMX0RelList(sen0, relResourceList, keyWordEventsList);
		_relationNodes.addRelList(tmx0List);
		// --tmx0 list
		
		// update on 7 July
		_relationNodes.updateRelationNodeListByFoundResource_extra(relResourceList);
		//--update on 7 July
		
		// update on 11 July
		// Time-Time list
		List<relationNodes> TimeTimeList=_relationNodes.loadTimeTimeRelList(_relationNodes.getRelNodesList(), relResourceList, keyWordEventsList);
		_relationNodes.addRelList(TimeTimeList);
		// --Time-Time list

		// update on 16 July
		// Equal Event list
		List<relationNodes> EqualEventList=_relationNodes.loadEquaRelListOIE5(keyWordEventsList);
		_relationNodes.addRelList(EqualEventList);
		// --Equal event list
		
		// Final relation list after optimizing
		_relationNodes.optimizeRelList();
		List<relationNodes> finalRelList=_relationNodes.getRelNodesList();
		
		// for final list (update Tense and Time)
		for (int i = 0; i < finalRelList.size(); i++) {
			relationNodes relNodes=finalRelList.get(i);
			relNodes.setTenseTime(sentenceList);
		}
		System.out.println();
		System.out.println("Final relations");
		for (int i = 0; i < finalRelList.size(); i++) {
			System.out.println(finalRelList.get(i).toString());
		}
		System.out.println();
		
		// Building graph
		// Cap nhat lai network cho graph, gaph chi go^`m causal relations va` cac temporal relations lien quan
		Graph graph = graphUtil.graphInit(finalRelList);
	    graph.display();
		// Print graph
		for (Node node : graph) {
            node.addAttribute("ui.label", node.getId());
        }
		
		// Building graph
		// Cap nhat lai network cho graph, gaph chi go^`m causal relations va` cac temporal relations lien quan
		
		List<relationNodes> finalRelList_update=_relationNodes.updateRelationNodeListByCausalResource(relResourceList, finalRelList);
		Graph graph1 = graphUtil.graphInit(finalRelList_update);
	    graph1.display();
		// Print graph
		for (Node node : graph1) {
            node.addAttribute("ui.label", node.getId());
        }
		
		// Evaluating on the graph
		// Load resource list
		
		System.out.println();
		System.out.println("Predicting...");
		
		// for tmx0 relation
		double outputNo=0;
		String outputResult="NONE";
		for (int i = 0; i < relCausalResourceList.size(); i++) {
			relationNodes relRes=relCausalResourceList.get(i);
			String node1Label=relRes.getLabel1();
			String node2Label=relRes.getLabel2();
			
			if (graph.getNode(node1Label)!=null&graph.getNode(node2Label)!=null) {
				Path shortestPath=graphUtil.shortestPath(graph, node1Label, node2Label);
				if (shortestPath!=null) {
					List<String> relPath=graphUtil.shortPathList(shortestPath);
					// for two node relation
					if (relPath.size()==1) {
						// extract two node relation from final relation list
						String[] relText=relPath.get(0).split("-");
						String node1Txt=relText[0].toLowerCase();
						String node2Txt=relText[1].toLowerCase();
						relationNodes pRelNodes=new relationNodes();
						// Xu ly cho reverse relation
						int reverseCondition=0;
						for (int j = 0; j < finalRelList.size(); j++) {
							if (finalRelList.get(j).isFound(node1Txt, node2Txt)==1) {
								pRelNodes=finalRelList.get(j);
								break;
								
							}else if (finalRelList.get(j).isReverse(node1Txt, node2Txt)==1) {
								pRelNodes=finalRelList.get(j);
								pRelNodes.reverse();
								reverseCondition=1;
								break;
							}
						}
						
						if (node1Txt.indexOf("tmx")==-1&&node2Txt.indexOf("tmx")==-1) {
							System.out.println(pRelNodes.toString());
							System.out.println("no time");
							outputResult=generalRules.twoNodesCausalRelation(pRelNodes, sentenceList);
							outputNo++;
							System.out.println(outputResult);
							System.out.println();
							//////////////////////
							if (reverseCondition==1) {
								outputResult=txtNormalization.reverseTxtCausal(outputResult);  // update 23 March
							}
							outputRecord.add(pColID+"\t"+node1Txt+"\t"+node2Txt+"\t"+outputResult);
						
						}					
					}else if (relPath.size()==2) {
						//  Can phai xu ly relation list cho path
						List<relationNodes> pathRelations=new ArrayList<>();
						/////////////////////////////////////////
						String[] firstNode=relPath.get(0).split("-");
						String firstnodeTxt=firstNode[0].toLowerCase();
						String[] lastNode=relPath.get(1).split("-");
						String lastnodeTxt=lastNode[1].toLowerCase();
						/////////////////////////////////////////

						for (int k = 0; k < relPath.size(); k++) {
							String[] relText=relPath.get(k).split("-");
							String node1Txt=relText[0].toLowerCase();
							String node2Txt=relText[1].toLowerCase();
							
							for (int j = 0; j < finalRelList.size(); j++) {
								relationNodes pRelTemp=finalRelList.get(j);
								if (pRelTemp.isFound(node1Txt, node2Txt)==1) {
									pathRelations.add(pRelTemp);
								}else if (pRelTemp.isReverse(node1Txt, node2Txt)==1) {
									pathRelations.add(pRelTemp);
								}
							}
						}
						
						System.out.println(relPath.toString());
						outputResult=generalRules.threeNodesCausalRelation(relPath, pathRelations, finalRelList, sentenceList, relCausalResourceList, relTempResourceList);
						outputNo++;
						System.out.println(outputResult);
						System.out.println();
						
						outputRecord.add(pColID+"\t"+firstnodeTxt+"\t"+lastnodeTxt+"\t"+outputResult);
						
					}else if (relPath.size()>2) {
						List<relationNodes> pathRelations=new ArrayList<>();
						/////////////////////////////////////////
						String[] firstNode=relPath.get(0).split("-");
						String firstnodeTxt=firstNode[0].toLowerCase();
						String[] lastNode=relPath.get(1).split("-");
						String lastnodeTxt=lastNode[1].toLowerCase();
						/////////////////////////////////////////

						for (int k = 0; k < relPath.size(); k++) {
							String[] relText=relPath.get(k).split("-");
							String node1Txt=relText[0].toLowerCase();
							String node2Txt=relText[1].toLowerCase();
							
							for (int j = 0; j < finalRelList.size(); j++) {
								relationNodes pRelTemp=finalRelList.get(j);
								if (pRelTemp.isFound(node1Txt, node2Txt)==1) {
									pathRelations.add(pRelTemp);
								}else if (pRelTemp.isReverse(node1Txt, node2Txt)==1) {
									pathRelations.add(pRelTemp);
								}
							}
						}
						
						System.out.println(relPath.toString());
						outputResult=generalRules.threeNodesCausalRelation(relPath, pathRelations, finalRelList, sentenceList, relCausalResourceList, relTempResourceList);
						outputNo++;
						System.out.println(outputResult);
						System.out.println();
						
						outputRecord.add(pColID+"\t"+firstnodeTxt+"\t"+lastnodeTxt+"\t"+outputResult);
					}
				}
			}
		}
		System.out.println(outputNo/relResourceList.size());
		System.out.println("----------------------------------------");
		System.out.println(finalRelList.size());
		System.out.println("----------------------------------------");
		
		return outputRecord;
	}	
	
	public static void start() throws IOException{
		
		// Load resource
		//String inputResouce="data//cdata//CausalEval3_1.TLINK"; // 1-original
		String inputResouce="data//cdata//CausalEval3_2.TLINK"; //2-extend
		BufferedReader br = new BufferedReader(new FileReader(inputResouce));
		String sentence = null;
		List<String> allSourceRecord=new ArrayList<>(); 
		while((sentence = br.readLine()) != null){
			allSourceRecord.add(sentence);
		}
		br.close();
		FileOutputStream fo = new FileOutputStream("data//cdata//oie5//output_files//2_ouput_results_oie5_voa");// 1-original(b); 2-extend(a)
		OutputStreamWriter out = new OutputStreamWriter(fo, "utf-8");
		
		String path="data//cdata//oie5//col_files//";
		String filetype="col";
		File dir = new File(path);
		File[] filesLL = dir.listFiles(new WildCardFileFilter("*."+filetype));
		for (int k = 0; k < filesLL.length; k++) {
			File f = filesLL[k];
			String inputCOL=f.getName();
			System.out.println("Loading..."+f.getName());
			List<Sentence> sentenceList = SentenceList("data//cdata//oie5//col_files//"+f.getName()) ;
			// Strating here
			String colID=f.getName();
			//process(sentenceList, allSourceRecord, colID);
			List<String> outputResult=processResult(sentenceList, allSourceRecord, colID);
			System.out.println();
			System.out.println("Output result");
			for (int i = 0; i < outputResult.size(); i++) {
				System.out.println(outputResult.get(i));
				out.write(outputResult.get(i));
				out.write("\n");
			}
		}
		out.close();
	}
	
	public static void main(String args[]) throws IOException{
		//testing();
		start();
	}
}
