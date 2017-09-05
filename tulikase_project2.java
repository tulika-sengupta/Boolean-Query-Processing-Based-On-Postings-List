package ir_proj2;

import java.awt.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Map.Entry;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class demo {

	public static HashMap<String, LinkedList>  invertedIndex = new HashMap();
	public static int comparison=0;
	public static int length_min_posting;
	public static int min_size;
	public static int index_pList;
	public static int[] minPListSize = new int[1000];
	public static int[] index = new int[1000];
	public static File f=null;
	public static PrintWriter pr=null;
	public static BufferedWriter br1=null;

	public static void main(String args[]) throws IOException{

		File inFile=null;
		String path=null,outputFile=null;
		if(0< args.length){
			path = args[0];
			outputFile=args[1];
			inFile = new File(args[2]);
		}
		BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(inFile),"UTF8"));
		try{

			f=new File(outputFile);
			FileOutputStream f1=new FileOutputStream(f);
			br1=new BufferedWriter(new OutputStreamWriter(f1, StandardCharsets.UTF_8));
			pr=new PrintWriter(br1);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		FileSystem fs = FileSystems.getDefault();
		Path path1 = fs.getPath(path);

		IndexReader reader = DirectoryReader.open(FSDirectory.open(path1));
		createInvertedIndex(reader);
		generateInvertedIndex(invertedIndex);

		//Reading the input file
		String line=null;
		while((line =br.readLine()) != null){
			ArrayList<LinkedList<Integer>> daatPostingsList = new ArrayList<LinkedList<Integer>>();    //daatPostingsList is an array of linkedlist which will contain the postings list of all the query terms
			String words[]=line.split(" ");     //words will contain the query terms
			printPostingsList(invertedIndex, words);
			taat_AND(invertedIndex,pr, words);
			taat_OR(words);
			for (int i=0;i<words.length;i++)
			{
				LinkedList<Integer> tempList = new LinkedList();
				tempList=(LinkedList<Integer>) invertedIndex.get(words[i]).clone();
				daatPostingsList.add(tempList);				
			}	
			sortPListSize(daatPostingsList);   //sorting the postings list according to its size to reduce the number of comparisons
			daat_AND(daatPostingsList, words); 
			daat_OR(daatPostingsList, words);
		}
		pr.close();
	}

	public static void printPostingsList(HashMap<String, LinkedList>  invertedIndex, String... terms){
		/*Printing the Posting Lists*/
		
		for(int x=0;x<terms.length;x++){
			
			pr.println("GetPostings ");
			LinkedList<Integer> postingsList = new LinkedList();
			postingsList=(LinkedList<Integer>) invertedIndex.get(terms[x]).clone();

			
			pr.println(terms[x]);

			//System.out.print( "Postings list: ");
			pr.print( "Postings list: ");

			for(int p=0;p<postingsList.size();p++)
			{ //System.out.print(postingsList.get(p) + " ");
			pr.print(postingsList.get(p) + " ");
			}
			//System.out.print("\n");
			pr.print("\n");
		}

	}

	public static void createInvertedIndex(IndexReader reader) throws IOException{

		FileWriter fw = new FileWriter("F:\\study\\IR\\Project2\\Proj2.txt"); //if we write true after filename then the true will append the new data

		//Fetching fields
		Fields fields = MultiFields.getFields(reader);
		int count=0;
		for(String field : fields)
		{
			if (!field.equals("_version_") && !field.equals("id")) 
			{

				Terms terms = fields.terms(field);
				TermsEnum termsEnum = terms.iterator();    //stream of tokens
				BytesRef t;

				while ( (t = termsEnum.next()) != null ) 
				{ 

					LinkedList<Integer> doc_Id = new LinkedList();
					String term=t.utf8ToString();

					PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader,
							field, t, PostingsEnum.FREQS);   //PostingsEnum- associated stream of docs
					int i;

					while ((i = postingsEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) 
					{

						int doc = postingsEnum.docID(); // The document
						int freq = postingsEnum.freq(); // Frequency of term in doc
						doc_Id.add(doc);

					}	 
					invertedIndex.put(term, doc_Id);
					count++;

					fw.write(count + " " + field + " ");//appends the string to the file
					fw.write(term + "\n");//appends the string to the file

				}	//end of while 	
			}// end of if	 
		}// end of for
	}

	public static void generateInvertedIndex(HashMap<String, LinkedList>  invertedIndex) throws IOException{
		FileWriter f_index = new FileWriter("F:\\study\\IR\\Project2\\InvertedIndex.txt");

		for (Entry<String, LinkedList> entry : invertedIndex.entrySet()) {
			//System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
			f_index.write("Key = " + entry.getKey() + " ");//appends the string to the file
			f_index.write("Value = " + entry.getValue()+ "\n");//appends the string to the file
		}
	}

	public static void taat_AND(HashMap<String, LinkedList>  invertedIndex, PrintWriter pr, String... terms ) throws IOException {
		// TODO Auto-generated method stub
		
		/* Taking two postings lists at a time and checking if there are any doc ids common and storing it in taatAND linked list. */
		LinkedList<Integer> postingsList1 = new LinkedList();
		LinkedList<Integer> postingsList2 = new LinkedList();
		LinkedList<Integer> taatAND = new LinkedList();
		int p;
		int comparison=0;

		postingsList1=(LinkedList<Integer>) invertedIndex.get(terms[0]).clone();      

		if(terms.length==1){

			taatAND=(LinkedList<Integer>) invertedIndex.get(terms[0]).clone();

		}
		else{
			g: for(int x=1;x<terms.length;x++){

				while(!taatAND.isEmpty()){
					taatAND.removeFirst();
				}			 
				postingsList2=(LinkedList<Integer>) invertedIndex.get(terms[x]).clone();

				int size1=postingsList1.size();
				int size2=postingsList2.size();

				int i=0,j=0;


				int index1=0;
				int index2=0;
				while(i<size1 && j<size2)
				{ 

					index1=postingsList1.get(i);
					index2=postingsList2.get(j);

					if (index1==index2)
					{
						i++;
						j++;

						taatAND.add(index1);
						comparison++;
					}

					else if (index1<index2)
					{
						i++;
						comparison++;
					}
					else if(index1>index2)
					{
						j++;
						comparison++;
					}
				}
				if(taatAND.size()!=0)
				{
					postingsList1=(LinkedList<Integer>) taatAND.clone();
				}
				else {
					//taatAND=null;

					break g;
				}

			}
		}

		//System.out.println("TaatAnd");
		pr.println("TaatAnd");
		for(int x=0;x<terms.length;x++){
			//System.out.print(terms[x]+" ");
			pr.print(terms[x]+" ");
		}

		//System.out.print("\nResults: ");
		pr.print("\nResults: ");
		if(taatAND.size()==0){
			//System.out.print("empty");
			pr.print("empty");
		}
		for(int i=0; i<taatAND.size();i++){
			//System.out.print(taatAND.get(i)+" ");
			pr.print(taatAND.get(i)+" ");
		}
		//System.out.println("Number of documents in results: "+taatAND.size());
		pr.println("Number of documents in results: "+taatAND.size());
		//System.out.println("Number of comparisons: "+ comparison);	
		pr.println("Number of comparisons: "+ comparison);
	}

	public static void taat_OR(String... terms){
		/*Taking two postings lists at a time and finding the smaller doc ids of both and adding it to the taatOr list */
		LinkedList<Integer> postingsList1 = new LinkedList();
		LinkedList<Integer> postingsList2 = new LinkedList();
		LinkedList<Integer> taatOR = new LinkedList();

		postingsList1=(LinkedList<Integer>) invertedIndex.get(terms[0]).clone();


		int index1=0;
		int index2=0;
		int comparison=0;

		if(terms.length==1){
			taatOR=(LinkedList<Integer>) invertedIndex.get(terms[0]).clone();
		}
		else{
			for(int x=1;x<terms.length;x++){

				int i=0, j=0;
				postingsList2=(LinkedList<Integer>) invertedIndex.get(terms[x]).clone();
				int size1=postingsList1.size();
				int size2=postingsList2.size();

				while(!taatOR.isEmpty()){
					taatOR.removeFirst();
				}	

				while(i<size1 && j<size2)
				{ 

					index1=postingsList1.get(i);
					index2=postingsList2.get(j);


					if (index1<index2)
					{
						taatOR.add(index1);
						i++;
						comparison++;
					}
					else if(index1>index2)
					{
						taatOR.add(index2);
						j++;
						comparison++;
					}
					else
					{
						taatOR.add(index2);
						i++;
						j++;
						comparison++;
					}
				}
				if(j>size2 && i>size1)
				{

				}

				while(i<size1)
				{
					index1=postingsList1.get(i);
					i++;
					taatOR.add(index1);

				}

				while(j<size2)
				{
					index2=postingsList2.get(j);
					j++;
					taatOR.add(index2);

				}
				postingsList1=(LinkedList<Integer>) taatOR.clone();
			}
		}

		//System.out.println("TaatOr");
		pr.println("TaatOr");
		for(int x=0;x<terms.length;x++){
			//System.out.print(terms[x]+" ");
			pr.print(terms[x]+" ");
		}
		//System.out.print("\nResults: ");
		pr.print("\nResults: ");
		for(int i=0; i<taatOR.size();i++){
			//System.out.print(taatOR.get((int) i)+ " ");
			pr.print(taatOR.get((int) i)+ " ");
		}
		//System.out.println("Number of documents in results: "+taatOR.size());
		pr.println("Number of documents in results: "+taatOR.size());
		//System.out.println("Number of comparisons: "+ comparison);	
		pr.println("Number of comparisons: "+ comparison);
	}
	public static void daat_OR(ArrayList<LinkedList<Integer>> daatPList, String... terms){
		
		/* Taking number of pointers equal to the number of terms and then simultaneously scanning all the lists. Finding the smallest id of all the doc ids and putting it in a variable "min". Then comparing all the doc ids pointed by the pointers and incrementing the pointer of the pointing list whose value is equal to min*/
		int len=0, i=0, j=0;
		int minDocId;
		int comparison=0;
		int[] pointer = new int[1000];
		LinkedList<Integer> docs = new LinkedList();


		Arrays.fill(pointer, 0);

		g: while(pointer[index[i]]<minPListSize[i] ){      // minPListSize array stores the sizes of the postings list in ascending order. index array stores the postings list sorted according to smallest size.

			minDocId=findMinDocId(daatPList,i,pointer, index);

			while(j<daatPList.size()){
				if(pointer[index[j]]<minPListSize[j]){
					if(daatPList.get(index[j]).get(pointer[index[j]]) <= minDocId){
						pointer[index[j]]++;                       //pointer array stores the pointers of the list.
						comparison++;
					}
				}
				j++;
			}
			docs.add(minDocId);
			len++;

			if(pointer[index[i]]>=minPListSize[i]){
				while(pointer[index[i]]>=minPListSize[i]){
					i++;
					j=i;
					if(i==daatPList.size()){
						break g;
					}
				}
			}
			else{
				j=0;
			}
		}	

		if(terms.length==1){
			comparison=0;
		}
		//System.out.println("DaatOr");
		pr.println("DaatOr");
		for(int x=0;x<terms.length;x++){
			//System.out.print(terms[x]+" ");
			pr.print(terms[x]+" ");
		}
		//System.out.print("\nResults: ");
		pr.print("\nResults: ");
		for(int x=0; x<docs.size();x++){
			//System.out.print(docs.get(x)+" ");
			pr.print(docs.get(x)+" ");
		}
		//System.out.println("Number of documents in results: "+docs.size());
		pr.println("Number of documents in results: "+docs.size());
		//System.out.println("Number of comparisons: "+ comparison);
		pr.println("Number of comparisons: "+ comparison);
	}

	public static int findMinDocId(ArrayList<LinkedList<Integer>> daatPList, int i, int[] pointer, int[] index){
		/* finding the minimum doc id*/
		int min = 1000000000;
		for(int x=0; x<daatPList.size(); x++){
			if(pointer[index[x]]<minPListSize[x]){
				int doc=daatPList.get(index[x]).get(pointer[index[x]]);
				if(min>=doc){
					min=doc;
				}
			}
		}
		return min;
	}
	public static void sortPListSize(ArrayList<LinkedList<Integer>> daatPList){

		int temp1; int temp2;

		for(int i=0; i<daatPList.size();i++){
			minPListSize[i]=daatPList.get(i).size();
			index[i]=i;
		}

		for(int i=0;i<daatPList.size();i++){
			for(int j=1;j<daatPList.size()-i;j++){
				if(minPListSize[j-1]>minPListSize[j]){
					temp1=minPListSize[j-1];
					minPListSize[j-1]=minPListSize[j];
					minPListSize[j]=temp1;

					temp2=index[j-1];
					index[j-1]=index[j];
					index[j]=temp2;

				}
			}
		}
	}

	public static void daat_AND(ArrayList<LinkedList<Integer>> daatPList, String... terms){
		
		/* Comparing all the postings lists simultaneously and comparing the first elements pointed by the postings list with first postings list. If there is a match then add it to daatAnd */
		LinkedList<Integer> docs = new LinkedList();
		int size_daatPlist = daatPList.size();
		int[] pointer = new int[1000];
		Arrays.fill(pointer, 0);

		int l= daatPList.get(0).size();

		int doc_Id;
		int count=0;
		int k=0;

		for(int i=0; i<l; i++){
			doc_Id=daatPList.get(0).get(i);        
			int j=1;

			while(j<size_daatPlist){			                              
				while( daatPList.get(j).get(pointer[j])<doc_Id ){             
					if(daatPList.get(j).size()-1>pointer[j])                   
					{
						pointer[j]++;                                             
						count++;
					}
					else{
						break;
						}

				}
				if(daatPList.get(j).get(pointer[j])==doc_Id){
					j++;
					count++;
				}
				else
					{
					count++;
					break;}
			}
			if(j==size_daatPlist){
				k++;
				docs.add(doc_Id);
			}
		}
		//System.out.println("DaatAnd");
		pr.println("DaatAnd");
		for(int x=0;x<terms.length;x++){
			//System.out.print(terms[x]+" ");
			pr.print(terms[x]+" ");
		}
		//System.out.print("\nResults: ");
		pr.print("\nResults: ");
		if(docs.size()==0){
			//System.out.print("empty");
			pr.print("empty");
		}
		for(int i=0; i<docs.size();i++){
			//System.out.print(docs.get(i)+" ");
			pr.print(docs.get(i)+" ");
		}
		//System.out.println("Number of documents in results: "+docs.size());
		pr.println("Number of documents in results: "+docs.size());
		//System.out.println("Number of comparisons: "+ count);	
		pr.println("Number of comparisons: "+ count);
	}
}


