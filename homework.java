import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class homework {

	public static List<HashMap<String, List<String>>> knowledgeBase = new ArrayList<>();
	
	public static void main(String[] args) {
		try {
			/* Reading the input */
			FileReader fr = new FileReader("input.txt");
			BufferedReader br = new BufferedReader(fr);
			
			int noOfQueries = Integer.parseInt(br.readLine());
			List<String> queries = new ArrayList<>();
			for(int i=0;i<noOfQueries;i++){
				queries.add(br.readLine().replaceAll(" ",""));
			}
			
			int noOfSentences = Integer.parseInt(br.readLine());
			List<String> sentences = new ArrayList<>();
			for(int i = 0;i<noOfSentences;i++){
				sentences.add(br.readLine().replaceAll(" ",""));
			}
			
			createKnowledgeBase(sentences);
			
			FileWriter fw = new FileWriter("output.txt");
			PrintWriter pw = new PrintWriter(fw);
			
			for(String query : queries){
				if(query.charAt(0)=='~'){
					query = query.substring(1);
				}
				else{
					query = "~"+query;
				}
				
				HashMap<String, List<String>> queryMap = new HashMap<>();
				List<String> queryList = new ArrayList<>();
				queryList.add(query);
				queryMap.put(getKeyOfPredicateTerm(query), queryList);
				knowledgeBase.add(queryMap);
				
				//Do resolution step
				if(resolution(query, knowledgeBase)){
					System.out.println("TRUE");
					pw.println("TRUE");
				}
				else{
					System.out.println("FALSE");
					pw.println("FALSE");
				}				
				knowledgeBase.remove(queryMap);
			}		
			br.close();
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void createKnowledgeBase(List<String> sentences) {
		int i = 0;
		for(String sentence : sentences){
			i++;
			HashMap<String, List<String>> hashmap = new HashMap<>();
			String[] terms = sentence.split("\\|");
			for(String term : terms){
				term = standardizeVariables(term,i);
				
				String key = getKeyOfPredicateTerm(term);
				if(hashmap.containsKey(key)){
					hashmap.get(key).add(term);
				}
				else{
					List<String> list = new ArrayList<>();
					list.add(term);
					hashmap.put(key, list);
				}
			}
			knowledgeBase.add(hashmap);
		}
	}

	private static String standardizeVariables(String term, int i) {
		List<String> values = getParameters(term);
		String newTerm = term.substring(0,term.indexOf("(")+1);
		for(String val : values){
			if(Character.isLowerCase(val.charAt(0))){
				newTerm +=val+i+",";
			}
			else{
				newTerm +=val+",";
			}
		}
		return newTerm.substring(0,newTerm.length()-1)+")";
	}

	private static String getKeyOfPredicateTerm(String term) {
		if(term.charAt(0)=='~'){
			term = term.substring(1);
		}
		return term.substring(0, term.indexOf('('));
	}
	
	private static boolean resolution(String query, List<HashMap<String, List<String>>> knowledgeBase) {
		
		String key = getKeyOfPredicateTerm(query);
		for(int i = 0;i < knowledgeBase.size();i++){
			HashMap<String, List<String>> sentence = knowledgeBase.get(i);
			int currentPredicate = 0;
			//find all the predicates whose key is same as that of the query
			if(sentence.containsKey(key)){
				List<String> predicateList = sentence.get(key);	//list of predicates whose key is same as that of the query
				
				boolean negativeQuery = false;
				if(query.charAt(0) == '~'){
					negativeQuery = true;
				}
				
				if(!negativeQuery){
					// search for a negative predicate
					int predicateNo = 0;
			        while(predicateNo < predicateList.size()){
			        	String predicate = predicateList.get(predicateNo);
			        	
			        	if(predicate.charAt(0) == '~'){
			            	List<String> substitutionList = unify(query, predicate);
			                if(substitutionList != null && !substitutionList.isEmpty()){
			                	List<String> predicateParameters = getParameters(predicate);
			                    HashMap<String, String> substitutionMap = new HashMap<>();
			                    int predicateParameterNo = 0;
			                    while(predicateParameterNo < predicateParameters.size()){
			                    	substitutionMap.put(predicateParameters.get(predicateParameterNo), substitutionList.get(predicateParameterNo));
			                    	predicateParameterNo++;
			                    }
			                    currentPredicate = predicateNo;
			                    List<String> generatedQueries = new ArrayList<>();
			                    // substituting the parameters of other predicates with different keys
			                    for(String keys : sentence.keySet()){
			                    	if(!keys.equalsIgnoreCase(key)){
			                    		List<String> otherPredicates = sentence.get(keys);
			                            for(String otherPredicate : otherPredicates){
			                            	generatedQueries.add(unification(substitutionMap, otherPredicate));
			                            }
			                    	}
			                    }
			                    // substituting the parameters of other predicates with same key
			                    int p = 0;
			                    while(p < predicateList.size()){
			                    	if(p != currentPredicate){
			                    		generatedQueries.add(unification(substitutionMap, predicateList.get(p)));
			                    	}
			                    	p++;
			                    } 
			                    if(generatedQueries.isEmpty()){
			                    	return true;
			                    }
			                    List<HashMap<String, List<String>>> tempKnowledgeBase = new ArrayList<>();
			                    tempKnowledgeBase.addAll(knowledgeBase);
			                    tempKnowledgeBase.remove(tempKnowledgeBase.get(i));
			                    
			                    int q = 0;
			                    while(q < generatedQueries.size()){
			                    	boolean ans = resolution(generatedQueries.get(q), tempKnowledgeBase);
			                    	if(!ans){
			                    		break;
			                    	}
			                    	q++;
			                    }
			                    if(q == generatedQueries.size()){
			                    	return true;	
			                    }
			                }                 
			            }        
			            predicateNo++;
			        }              
				}
				else{
					// search for a positive predicate
					int predicateNo = 0;
			        while(predicateNo < predicateList.size()){
			        	String predicate = predicateList.get(predicateNo);
			        	if(predicate.charAt(0) != '~'){
			            	List<String> substitutionList = unify(query, predicate);
			                if(substitutionList != null && !substitutionList.isEmpty()){
			                	List<String> predicateParameters = getParameters(predicate);
			                    HashMap<String, String> substitutionMap = new HashMap<>();
			                    int predicateParameterNo = 0;
			                    while(predicateParameterNo < predicateParameters.size()){
			                    	substitutionMap.put(predicateParameters.get(predicateParameterNo), substitutionList.get(predicateParameterNo));
			                    	predicateParameterNo++;
			                    }
			                    currentPredicate = predicateNo;
			                    List<String> generatedQueries = new ArrayList<>();
			                    // substituting the parameters of other predicates with different keys
			                    for(String keys : sentence.keySet()){
			                    	if(!keys.equalsIgnoreCase(key)){
			                    		List<String> otherPredicates = sentence.get(keys);
			                            for(String otherPredicate : otherPredicates){
			                            	generatedQueries.add(unification(substitutionMap, otherPredicate));
			                            }
			                    	}
			                    }
			                    // substituting the parameters of other predicates with same key
			                    int p = 0;
			                    while(p < predicateList.size()){
			                    	if(p != currentPredicate){
			                    		generatedQueries.add(unification(substitutionMap, predicateList.get(p)));
			                    	}
			                    	p++;
			                    }  
			                    if(generatedQueries.isEmpty()){
			                    	return true;
			                    }
			                    
			                    List<HashMap<String, List<String>>> tempKnowledgeBase = new ArrayList<>();
			                    tempKnowledgeBase.addAll(knowledgeBase);
			                    tempKnowledgeBase.remove(tempKnowledgeBase.get(i));
			                    
			                    int q = 0;
			                    while(q < generatedQueries.size()){
			                    	boolean ans = resolution(generatedQueries.get(q), tempKnowledgeBase);
			                    	if(!ans){
			                    		break;
			                    	}
			                    	q++;
			                    }
			                    if(q == generatedQueries.size()){
			                    	return true;	
			                    }
			                }                
			            }        
			            predicateNo++;
			        }
				}
			}
		}
		return false;
	}

	private static String unification(HashMap<String, String> substitutionMap, String otherPredicate) {
		String temp = otherPredicate.substring(0, otherPredicate.indexOf('(')+1);
	    List<String> substitutionList = getParameters(otherPredicate);
	    for(String s : substitutionList){
	    	if(substitutionMap.containsKey(s)){
	        	temp += substitutionMap.get(s) + ",";
	        }
	        else{
	        	temp += s + ",";
	        }
	    }
		temp += ")";
	    return temp;
	}

	private static List<String> getParameters(String string) {
		String temp = string.substring(string.indexOf("(")+1, string.length()-1);
	    String tempArr[] = temp.split(",");
	    List<String> list = new ArrayList<>();
	    for(String s : tempArr){
	    	list.add(s);
	    }
	    return list;
	}

	private static List<String> unify(String query, String predicate) {
		List<String> substitutionList = new ArrayList<>();
		List<String> queryParametersList = getParameters(query);
		List<String> predicateParametersList = getParameters(predicate);
	    HashMap<String, String> substitutionMap = new HashMap<>();
		int i = 0;
		if(queryParametersList.size() == predicateParametersList.size()){
			while(i < queryParametersList.size()){
				if(Character.isUpperCase(queryParametersList.get(i).charAt(0)) && Character.isUpperCase(predicateParametersList.get(i).charAt(0))){  
					// if both starts with upper case
					if(queryParametersList.get(i).equals(predicateParametersList.get(i))){
						substitutionList.add(queryParametersList.get(i));
					}
					else{
						break;
					}
				}
				else if(Character.isUpperCase(queryParametersList.get(i).charAt(0)) && Character.isLowerCase(predicateParametersList.get(i).charAt(0))){
					// if query parameter starts with upper case and predicate with lower case
					if(!substitutionMap.containsKey(predicateParametersList.get(i))){
						substitutionMap.put(predicateParametersList.get(i), queryParametersList.get(i));
					}
					else {
						if(!substitutionMap.get(predicateParametersList.get(i)).equals(queryParametersList.get(i))){
							break;
						}
					}
					substitutionList.add(queryParametersList.get(i));
				}
				else if(Character.isLowerCase(queryParametersList.get(i).charAt(0)) && Character.isUpperCase(predicateParametersList.get(i).charAt(0))){
					// if query parameter starts with upper case and predicate with lower case
					if(!substitutionMap.containsKey(queryParametersList.get(i))){
						substitutionMap.put(queryParametersList.get(i), predicateParametersList.get(i));
					}
					else {
						if(!substitutionMap.get(queryParametersList.get(i)).equals(predicateParametersList.get(i))){
							break;
						}
					}
					substitutionList.add(predicateParametersList.get(i));
				}
				else{
					// if both are lower case
					if(!substitutionMap.containsKey(queryParametersList.get(i))){
						substitutionMap.put(queryParametersList.get(i), predicateParametersList.get(i));
					}
					else {
						if(!substitutionMap.get(queryParametersList.get(i)).equals(predicateParametersList.get(i))){
							break;
						}
					}
					substitutionList.add(predicateParametersList.get(i));
				}
				i++;
			}
			if(i < queryParametersList.size()){
	            return null;
	        }
		}
		return substitutionList;
	}
}