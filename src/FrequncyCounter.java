import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.data.list.PointerTargetTreeNode;
import net.didion.jwnl.data.list.PointerTargetTreeNodeList;
import net.didion.jwnl.dictionary.Dictionary;
import net.didion.jwnl.dictionary.MorphologicalProcessor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by Nisansa on 15/03/06.
 */
public class FrequncyCounter {

    HashMap<String,WordStat> wordStats=new HashMap<String,WordStat>();
    ArrayList<Sentence> sentences=new  ArrayList<Sentence>();
    ArrayList<Sentence> testSentences=new  ArrayList<Sentence>();
    ArrayList<Sentence> train=new  ArrayList<Sentence>();
    ArrayList<Sentence> test=new  ArrayList<Sentence>();
    ArrayList<Integer[]> assignments=new  ArrayList<Integer[]>();
    Stemmer stemmer=null;
    int[] allStats=new int[5];

    ILexicalDatabase db = new NictWordNet();
    private RelatednessCalculator rc= new WuPalmer(db);
    ArrayList<String> availableWords=new  ArrayList<String>();
    static double saturationAmount=0.935;
    static double idfW=1;

    boolean realTest=false;

    public static void main(String[] args) {


        FrequncyCounter fc = new FrequncyCounter();
        String path = "./";
        fc.readDataFile(path + "train.tsv", false);
        fc.readTestDataFile(path + "test.tsv", false);
        //fc.randomPartition(0.1);
        //fc.runIteration();


       // for (; saturationAmount < 0.941; saturationAmount=saturationAmount+0.001) {
           // fc.nFoldCrossValidation(10);
       // }
       // for (idfW = 1; idfW <2.1 ; idfW=idfW+0.1) {
                  // fc.nFoldCrossValidation(10);
                  fc.Calssify();
                fc.writeClassifications(path + "fin.csv");
        //}

        //System.out.println(fc);


        //c.writeFile(path);
    }




    public void runIteration(){
        train();
        normalize();
        System.out.println(evaluate(test));
    }

    public void train(){
        for (int i = 0; i < train.size(); i++) {
            Sentence s=train.get(i);
            ProcessLine(s.phraseId, s.sentenceId, s.sentenceParts,s.classVal);
        }
    }


    public void Calssify(){
        wordStats=new HashMap<String,WordStat>();
        for (int i = 0; i < sentences.size(); i++) {
            train.add(sentences.get(i));
        }
        for (int i = 0; i < testSentences.size(); i++) {
            test.add(testSentences.get(i));
        }

        double trainAccuracy=0;
        double testAccuracy=0;
        train();
        normalize();
        trainAccuracy+=evaluate(train);
        testAccuracy+=evaluate(test);


        //System.out.println(saturationAmount);
        //System.out.println(idfW);
        System.out.println("Train Accuracy: "+trainAccuracy);
       // System.out.println("Test Accuracy: "+testAccuracy); //Does not have a real meaning.
    }


   public void writeClassifications(String fullFilePath){
        try {
            File statText = new File(fullFilePath);
            FileWriter fw=new FileWriter(statText,true);
            // FileOutputStream is = new FileOutputStream(statText);
            // OutputStreamWriter osw = new OutputStreamWriter(is);
            BufferedWriter w = new BufferedWriter(fw);
            w.write("PhraseId,Sentiment\n");
            for (int i = 0; i <assignments.size() ; i++) {
                Integer[] res=assignments.get(i);
                w.write(res[0]+","+res[1]+"\n");
            }
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing lines to the file "+fullFilePath);
        }
    }

    public void nFoldCrossValidation(int n){
        double trainAccuracy=0;
        double testAccuracy=0;
        for (int i = 0; i <n ; i++) {
            wordStats=new HashMap<String,WordStat>();
            train=new  ArrayList<Sentence>();
            test=new  ArrayList<Sentence>();

            ArrayList<Sentence> tempSentences=new  ArrayList<Sentence>();
            tempSentences.addAll(sentences);
            int limit=(int)(sentences.size()/n);
            for (int j = 0; j < i*limit; j++) {
                train.add(tempSentences.get(j));
            }
            int cut=((i*limit)+limit);
            for (int j = i*limit; j <cut ; j++) {
                test.add(tempSentences.get(j));
            }
            for (int j = cut; j <tempSentences.size() ; j++) {
                train.add(tempSentences.get(j));
            }
            train();
            normalize();
            trainAccuracy+=evaluate(train);
            testAccuracy+=evaluate(test);
        }
        //System.out.println(saturationAmount);
        System.out.println(idfW);
        System.out.println("Train Accuracy: "+trainAccuracy/n);
        System.out.println("Test Accuracy: "+testAccuracy/n);
    }

    public void randomPartition(double trainFraction){
        ArrayList<Sentence> tempSentences=new  ArrayList<Sentence>();
        tempSentences.addAll(sentences);
        int limit=(int)(trainFraction*sentences.size());
        Random r=new Random();
        for (int i = 0; i <limit ; i++) {
            int index=r.nextInt(tempSentences.size());
            train.add(tempSentences.get(index));
            tempSentences.remove(index);
        }
        test.addAll(tempSentences);
    }

    public double evaluate(ArrayList<Sentence> evalSentences){
        missingMissed=0;
        missingHit=0;
        missingMissVarience =0;
        foundMissed=0;
        foundHit=0;
        foundMissVarience =0;
        assignments=new  ArrayList<Integer[]>();


        int count=0;
        for (int i = 0; i < evalSentences.size(); i++) {
            if(evaluate(evalSentences.get(i))){
                count++;
            }
        }
        double misVar=0;
        double foundVar=0;
        double missingSum=missingMissed+missingHit;
        double foundSum=foundMissed+foundHit;
        double missPer=0;
        double foundPer=0;
        if(missingMissed>0){
            misVar= missingMissVarience /missingMissed;
        }
        if(foundMissed>0){
            foundVar= foundMissVarience /foundMissed;
        }
        if(missingSum>0){
            missPer=(missingMissed*100)/(missingMissed+missingHit);
        }
        if(foundSum>0){
            foundPer=(foundMissed*100)/(foundMissed+foundHit);
        }
       // System.out.println("Missing::  Miss: "+missingMissed+" Hit: "+missingHit+" Miss Per%: "+missPer+" Miss Var: "+ misVar);
       // System.out.println("Found::  Miss: "+foundMissed+" Hit: "+foundHit+" Miss Per%: "+foundPer+" Miss Var: "+ foundVar);

        return(((double)(count*100))/evalSentences.size());
    }

    int missingMissed=0;
    int missingHit=0;
    double missingMissVarience =0;
    int foundMissed=0;
    int foundHit=0;
    double foundMissVarience =0;

    public boolean evaluate(Sentence s){
        double[] values=new double[5];
        ArrayList<String> ngrams=new ArrayList<String>();
        for (int i = 0; i <s.sentenceParts.length ; i++) {
            ngrams.add(s.sentenceParts[i]);
        //    if(i!=0) {
         //       ngrams.add(s.sentenceParts[i - 1] + " " + s.sentenceParts[i]);
         //   }
        }


        boolean wordMissing=false;

        for (int i = 0; i <ngrams.size(); i++) {
            String word=ngrams.get(i);
            WordStat ws=wordStats.get(word);
            double wordnetModifier=1;
            if(ws==null) {
                wordMissing=true;
                //Wordnet
                SimilarityElement se=getClosest(word);

                    ws = se.ws;
                    wordnetModifier =Math.max(saturationAmount, se.similarity);  //0.9

               // ws=null; //Turn off wordnet matching
            }




           // wordnetModifier=1;  //Turn off wordnet scalling
           // idfW=1; //Turn off weight

            if(ws!=null) {
                //ws.infoModifier=0; //Turn off info

                double[] partValues=ws.classStats;
                for (int j = 0; j < values.length; j++) {
                    values[j]+=wordnetModifier*(idfW*ws.IDFmodifier +ws.infoModifier)*partValues[j];
                }
            }

        }

        double max=0;
        int index=2;
        double secondMax=0;
        int secondMaxIndex=0;
        for (int i = 0; i <values.length; i++) {
            if(max<values[i]){
                secondMax=max;
                secondMaxIndex=index;
                max=values[i];
                index=i;
            }
        }




        //If the second guess is better than 99.999% of the best, try a weighted random guess between the two
       /* if(secondMax>=(max*0.99999)) {
          //  System.out.println(secondMax+" "+max);
            int maxInt=(int)(100*max);
            int secondInt=(int)(100*secondMax);
            int sum=maxInt+secondInt;
           // System.out.println(secondInt+" "+maxInt);
            if(sum>0) {
                Random r = new Random();
                int guess = r.nextInt(sum);
                if (guess > maxInt) {
                    index = secondMaxIndex;
                }
            }
        }*/
        if(realTest && allStats[secondMaxIndex]>allStats[index] && secondMax>=(max*0.99)){
            index=secondMaxIndex;
        }

        assignments.add(new Integer[]{s.phraseId,index});

        if(wordMissing){
            if(s.classVal==index) {
                missingHit++;
            }
            else{
                missingMissed++;
                missingMissVarience +=Math.abs(s.classVal-index);
            }
        }
        else{
            if(s.classVal==index) {
                foundHit++;
            }
            else{
                foundMissed++;
                foundMissVarience +=Math.abs(s.classVal-index);
            }
        }
        return (s.classVal==index);
        //System.out.println(s.classVal+" -> "+index);
    }

    private SimilarityElement getClosest(String word){
        if(availableWords==null){
            availableWords=new ArrayList<String>();
            availableWords.addAll(wordStats.keySet());
        }
        double max=0;
        String best="";
        String[] type=new String[]{"#n","#v","#other"};
        int j=0;

        do {
            for (int i = 0; i < availableWords.size(); i++) {
                String compareTo = availableWords.get(i);
                double value = rc.calcRelatednessOfWords(word + type[j], compareTo + type[j]);
                if (max < value) {
                    max = value;
                    best = compareTo;
                }
            }
            j++;
        }while(max==0 && j<type.length);


        return new SimilarityElement(wordStats.get(best),max);
    }

    public class SimilarityElement{
        WordStat ws;
        double similarity=0;

        public SimilarityElement(WordStat ws, double similarity) {
            this.ws = ws;
            this.similarity = Math.min(similarity,1); //To handle Errones case of the same word getting more than 1 for similarity. This is not a problem here because logically, if the word was already there, we would not have to check the wordnet similarity anyway
        }
    }


    public String toString(){
        Iterator<WordStat> itr=wordStats.values().iterator();
        StringBuilder sb=new StringBuilder();
        while(itr.hasNext()){
            sb.append(itr.next().toString());
        }
        return sb.toString();
    }

    public void normalize() {
        Iterator<WordStat> itr = wordStats.values().iterator();
        double[] modifiers = new double[wordStats.values().size()];
        double[] entModifiers = new double[wordStats.values().size()];
        int i = 0;
        while (itr.hasNext()) {
            WordStat ws = itr.next();
            ws.setClassStats(sumNormalize(ws.getClassStats()));
           // System.out.println(ws.toString());
            modifiers[i] = logb(sentences.size() / (1 + ws.sentenceCount), 10);
            entModifiers[i]=1-calculateEntropy(ws.getClassStats());//((ws.getNonZeroCount()* calculateEntropy(ws.getClassStats()))/ws.getClassStats().length);//calculateEntropy(ws.getClassStats());
            i++;
        }
      //  modifiers=standadize(modifiers);
        modifiers = normalize(modifiers);
       // entModifiers=standadize(entModifiers);
        entModifiers=normalize(entModifiers);
        itr = wordStats.values().iterator();
        i = 0;
        while (itr.hasNext()) {
            WordStat ws = itr.next();
            ws.IDFmodifier =modifiers[i];
            ws.infoModifier=entModifiers[i];//1;//calculateEntropy(ws.getClassStats());//1;//((ws.getNonZeroCount()* calculateEntropy(ws.getClassStats()))/ws.getClassStats().length);
            i++;
           // System.out.println(ws.toString());
        }
    }



   /* public double[] standadize(double[] stats){
        double mean=0;
        for (int i = 0; i < stats.length; i++) {
            mean+=stats[i];
        }
        mean=mean/stats.length;
        double stanDev=0;
        for (int i = 0; i <stats.length ; i++) {
            stanDev+=Math.pow(stats[i]-mean,2);
        }
        stanDev=stanDev/stats.length;
        stanDev=Math.sqrt(stanDev);
        double[] newStats=new double[stats.length];
        for (int i = 0; i < stats.length; i++) {
            newStats[i]=(stats[i]-mean)/stanDev;
        }
        return newStats;
    }*/

    public double[] standadize(double[] stats){
        double mean=0;
        for (int i = 0; i < stats.length; i++) {
            mean+=stats[i];
        }
        mean=mean/stats.length;
        double stanDev=0;
        for (int i = 0; i <stats.length ; i++) {
            stanDev+=Math.pow(stats[i]-mean,2);
        }
        stanDev=stanDev/stats.length;
        stanDev=Math.sqrt(stanDev);
        double b=2*Math.pow(stanDev,2);
        double denom=stanDev*Math.sqrt(2*Math.PI);

        double[] newStats=new double[stats.length];
        for (int i = 0; i < stats.length; i++) {
            double pow=(-Math.pow(stats[i]-mean,2))/b;
            double numerator=Math.exp(pow);
            newStats[i]=numerator/denom;
        }
        return newStats;
    }

    public double[] sumNormalize(double[] stats){
        double sum=0;
        for (int i = 0; i < stats.length; i++) {
            sum+=stats[i];
        }
        double[] newStats=new double[stats.length];
        for (int i = 0; i < newStats.length; i++) {
            newStats[i]=(stats[i]/sum);
        }
        return newStats;
    }

    public double[] normalize(double[] stats){
        double curmMin=Double.MAX_VALUE;
        double curMax=-Double.MAX_VALUE;
        double[] newStats=new double[stats.length];
        for (int i = 0; i < stats.length; i++) {
            curmMin=Math.min(curmMin,stats[i]);
            curMax=Math.max(curMax, stats[i]);
        }
        //System.out.println(curmMin+" "+curMax);
        double tarMin=Math.min(Double.MIN_VALUE,Math.max(curmMin,0)); //If zero keep at zero, if negative set to zero, otherwise set to min val
        double tarMax=1;
        //System.out.println(tarMin+" "+tarMax);
        double m=(tarMax-tarMin)/(curMax-curmMin);
        double c=((curmMin*tarMax)-(curMax*tarMin))/(curmMin-curMax);
        for (int i = 0; i <stats.length; i++) {
            newStats[i]=m*stats[i]+c;
        }
        return newStats;
    }



    public double calculateEntropy(double[] nums){
        double sum=0;
        for (int i = 0; i < nums.length; i++) {
            sum+=nums[i];
        }
        return (calculateEntropy( nums, sum));
    }

    public double calculateEntropy(double[] nums, double sum){
        double val=0;
        for (int i = 0; i < nums.length; i++) {
            val+=calculateInfo(nums[i], sum);
        }
        return (val);
    }

    private double calculateInfo(int a, int b){
        return(calculateInfo((double) a,(double) b));
    }

    private double calculateInfo(double a, double b){
        double ratio=a/b;
        if (ratio>0) {
            return (-ratio * log2(ratio));
        }
        else{
            return 0;
        }
    }

    public double log2( double a )
    {
        return logb(a,2);
    }

    public double logb( double a, double b )
    {
        return Math.log(a) / Math.log(b);
    }

    public void readDataFile(String path, Boolean isSpam) {
        String line=null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            line = br.readLine();
            line = br.readLine(); //Ignoring the first line
            int messagePartIndex = 0;
            StringBuilder sb = new StringBuilder();
            //Phrase m = null;
            while (line != null) {
                //  System.out.println(line);
                String[] parts=line.split("\t");
                //m=new Phrase(parts);
                String[] processedParts=breakLineToParts(parts[2]);
                if(processedParts.length>0) {
                    Sentence s=new Sentence(parts[0], parts[1], parts[2], processedParts, parts[3]);
                    sentences.add(s);
                    allStats[s.classVal]++;
                }
                //unigrams.addAll(m.getUnigrams());
                //bigrams.addAll(m.getBigrams());
               // phrases.add(m);
             /*   if (messagePartIndex == 0) { // Sender email
                    m = new Phrase(line, isSpam);
                    hosts.add(m.getHost());
                    messagePartIndex++;
                } else if (messagePartIndex == 1) {
                    messagePartIndex++; // Ignore the timeStamp
                } else if (messagePartIndex == 2) { // Subject
                    m.setSubject(line);
                    messagePartIndex++;
                } else {
                    if (!line.contains("&&&&&&&")) {
                        sb.append(line);
                        sb.append(" ");
                    } else {
                        m.setMessage(sb.toString());
                        unigrams.addAll(m.getUnigrams());
                        bigrams.addAll(m.getBigrams());
                        phrases.add(m);
                        System.out.println(m.toString());
                        messagePartIndex = 0; // reset message part counter
                        sb = new StringBuilder(); // Prepare string builder for next message
                    }
                }*/
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }



    }

    public void readTestDataFile(String path, Boolean isSpam) {
        realTest=true;
        String line=null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            line = br.readLine();
            line = br.readLine(); //Ignoring the first line
            int messagePartIndex = 0;
            StringBuilder sb = new StringBuilder();
            //Phrase m = null;
            while (line != null) {
                //  System.out.println(line);
                String[] parts=line.split("\t");
                //m=new Phrase(parts);
                String[] processedParts=breakLineToParts(parts[2]);
                if(processedParts.length>0) {
                    testSentences.add(new Sentence(parts[0], parts[1], parts[2], processedParts,"2"));
                }
                else{
                    testSentences.add(new Sentence(parts[0], parts[1], parts[2], new  String[]{},"2"));
                }
                //unigrams.addAll(m.getUnigrams());
                //bigrams.addAll(m.getBigrams());
                // phrases.add(m);
             /*   if (messagePartIndex == 0) { // Sender email
                    m = new Phrase(line, isSpam);
                    hosts.add(m.getHost());
                    messagePartIndex++;
                } else if (messagePartIndex == 1) {
                    messagePartIndex++; // Ignore the timeStamp
                } else if (messagePartIndex == 2) { // Subject
                    m.setSubject(line);
                    messagePartIndex++;
                } else {
                    if (!line.contains("&&&&&&&")) {
                        sb.append(line);
                        sb.append(" ");
                    } else {
                        m.setMessage(sb.toString());
                        unigrams.addAll(m.getUnigrams());
                        bigrams.addAll(m.getBigrams());
                        phrases.add(m);
                        System.out.println(m.toString());
                        messagePartIndex = 0; // reset message part counter
                        sb = new StringBuilder(); // Prepare string builder for next message
                    }
                }*/
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }



    }

    public class Sentence{
        String[] sentenceParts;
        int classVal;
        int phraseId=0;
        int sentenceId=0;
        String sentence="";

        public Sentence(String phraseIdS, String	sentenceIdS,String sentenceS,String[] sentenceParts, String classVal) {
            this( phraseIdS,sentenceIdS,sentenceS,sentenceParts,Integer.parseInt(classVal));
        }
        public Sentence(String phraseIdS, String	sentenceIdS,String sentenceS,String[] sentenceParts, int classVal) {
            this.sentenceParts = sentenceParts;
            this.classVal = classVal;
            phraseId=Integer.parseInt(phraseIdS);
            sentenceId=Integer.parseInt(sentenceIdS);
            sentence=sentenceS;
        }
    }

    public String[] breakLineToParts(String	phraseS){
        phraseS=phraseS.replaceAll("…","...");
        phraseS=phraseS.replaceAll("([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\|\\)\\]])"," ");
        phraseS=phraseS.replaceAll(","," ");
        phraseS=phraseS.replaceAll("_"," ");
        phraseS=phraseS.replaceAll("-"," ");
        phraseS=phraseS.replaceAll(":"," ");
        phraseS=phraseS.replaceAll(";"," ");
        phraseS=phraseS.replaceAll("!"," ");
        phraseS=phraseS.replaceAll("/"," ");
        phraseS=phraseS.replaceAll("\""," ");
        phraseS=phraseS.replaceAll("“"," ");
        phraseS=phraseS.replaceAll("”"," ");
        phraseS=phraseS.replaceAll("="," ");
        phraseS=phraseS.replaceAll("'"," ");
        phraseS=phraseS.replaceAll("%"," %");
        phraseS=phraseS.replaceAll("( )+"," ");
        return(phraseS.split(" "));
    }

    public String[] ProcessLine(int phraseId, int	sentenceId, String[] parts,int	sentimentS){
        //int phraseId=phraseId;
      //  int sentenceId=sentenceId;
        int sentiment=sentimentS;
        //phraseStat=new StringStat(phraseS);



        //Count words (uni-grams)
        HashMap<String,Integer> wordcounts=new HashMap<String,Integer>();
        for (int i = 0; i < parts.length; i++) {
            Integer count=wordcounts.get(parts[i]);
            if(count==null){
                wordcounts.put(parts[i],1);
            }
            else{
                count++;
                wordcounts.put(parts[i],count);
            }
        }

 /*       //Count words (bi-grams)
        for (int i = 0; i < parts.length-1; i++) {
            String bigram=parts[i]+" "+parts[i+1];
            Integer count=wordcounts.get(bigram);
            if(count==null){
                wordcounts.put(bigram,1);
            }
            else{
                count++;
                wordcounts.put(bigram,count);
            }
        }*/

        //Calculate the frequency for each word
        HashMap<String,Double> wordfequncies=new HashMap<String,Double>();
        Double maxFrequncy=0.0;
        Iterator<String> itr=wordcounts.keySet().iterator();
        while(itr.hasNext()){
            String word=itr.next();
            Double fr=((double)(wordcounts.get(word))/(double)parts.length);
            maxFrequncy=Math.max(maxFrequncy,fr);
            wordfequncies.put(word,fr);
        }

        //Add to the word frequency
        itr=wordfequncies.keySet().iterator();
        while(itr.hasNext()) {
            String word = itr.next();
            WordStat ws=wordStats.get(word);
            if(ws==null){
                ws=new WordStat(word);
            }
            else{
                wordStats.remove(word);
            }
            ws.updateStat(sentiment,(0.5*wordfequncies.get(word))/maxFrequncy);
            wordStats.put(word,ws);
        }
        return parts;

    }
    
    
    public class WordStat{
        String word;
        private double[] classStats =new double[5];
        double IDFmodifier =1;
        double infoModifier=1;
        int sentenceCount=0;
        int nonZeroCount =-1;

        public WordStat(String word) {
            this.word = word;
        }

        public void updateStat(int classIndex, double value){
            getClassStats()[classIndex]+=value;
            sentenceCount++;
        }

        public String toString(){
            StringBuilder sb=new StringBuilder();
            sb.append(word);
            sb.append(" : ");
            sb.append(IDFmodifier);
            sb.append(" : ");
            for (int i = 0; i < getClassStats().length; i++) {
                sb.append(getClassStats()[i]);
                sb.append(" ");
            }
            sb.append("\n");
            return  sb.toString();
        }

        public int getNonZeroCount(){
            if(nonZeroCount ==-1){
                nonZeroCount =0;
                for (int i = 0; i < classStats.length; i++) {
                    if(classStats[i]!=0){
                        nonZeroCount++;
                    }
                }
            }
            return nonZeroCount;
        }

        public double[] getClassStats() {
            return classStats;
        }

        public void setClassStats(double[] classStats) {
            this.classStats = classStats;
        }
    }




    public class Stemmer{
        private int MaxWordLength = 50;
        private Dictionary dic;
        private MorphologicalProcessor morph;
        private boolean IsInitialized = false;
        public HashMap<String,String> AllWords = null;
        Dictionary dictionary =null;
        PointerUtils p =null;

        /**
         * establishes connection to the WordNet database
         */
        public Stemmer ()
        {
            AllWords = new HashMap<String,String>();
            dictionary = Dictionary.getInstance();
            p = PointerUtils.getInstance();

            try
            {
                // JWNL.initialize(new FileInputStream("file_properties.xml"));
                JWNLConnecter.initializeJWNL();
                dic = Dictionary.getInstance();
                morph = dic.getMorphologicalProcessor();
                // ((AbstractCachingDictionary)dic).
                //	setCacheCapacity (10000);
                IsInitialized = true;
            }
            catch (Exception e){
                System.err.println(e.getMessage());
            }
            /*catch ( FileNotFoundException e )
            {
                System.out.println ( "Error initializing Stemmer: JWNLproperties.xml not found" );
            }
            catch ( JWNLException e )
            {
                System.out.println ( "Error initializing Stemmer: "
                        + e.toString() );
            }*/

        }

        public void Unload ()
        {
            dic.close();
            Dictionary.uninstall();
            JWNL.shutdown();
        }


        public String getBaseWord(String sWord,int nounDepth){
            int depth=0;
            dictionary = Dictionary.getInstance();
            p = PointerUtils.getInstance();

            try {
                IndexWord word = dictionary.lookupIndexWord(POS.VERB, sWord);
                if(word==null){
                    word = dictionary.lookupIndexWord(POS.NOUN, sWord);
                    depth=nounDepth;
                }
                if(word==null){
                    word = dictionary.lookupIndexWord(POS.ADJECTIVE, sWord);
                    if(word!=null){
                        return sWord;
                    }
                }
                if(word==null){
                    word = dictionary.lookupIndexWord(POS.ADVERB, sWord);
                    if(word!=null){
                        return sWord;
                    }
                }
                if(word==null){
                    return null; //If not in wordnet ignore word
                }

                // System.out.println (sWord+" s "+word);
                Synset[] s = word.getSenses(); //give word here
                if(s.length==0){
                    return sWord;
                }


                // for (int i = 0; i < s.length; i++) {
                PointerTargetTree pt = p.getHypernymTree(s[0],depth);  //down
                if(pt==null){
                    System.out.println ("null");
                }
                PointerTargetTreeNode rootNode = pt.getRootNode();
                PointerTargetTreeNodeList l= null;
                PointerTargetTreeNode nextNode=null;
                for (int j = 0; j < depth; j++) {
                    l= rootNode.getChildTreeList();
                    if(l!=null) {
                        nextNode = (PointerTargetTreeNode) l.get(0);
                    }
                    if (nextNode != null) {
                        rootNode = nextNode;
                    }
                }



                return(rootNode.getSynset().getWord(0).getLemma());
                // }
            } catch (JWNLException ex) {
                //System.out.println ( "Error " );
                //Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (Exception e){

            }
            return null; //If not in wordnet ignore word
        }




        /* stems a word with wordnet
        * @param word word to stem
        * @return the stemmed word or null if it was not found in WordNet
        */
        public String StemWordWithWordNet ( String word )
        {
            if ( !IsInitialized )
                return word;
            if ( word == null ) return null;
            if ( morph == null ) morph = dic.getMorphologicalProcessor();

            IndexWord w;
            try
            {
                w = morph.lookupBaseForm( POS.VERB, word );
                if ( w != null )
                    return w.getLemma().toString ();
                w = morph.lookupBaseForm( POS.NOUN, word );
                if ( w != null )
                    return w.getLemma().toString();
                w = morph.lookupBaseForm( POS.ADJECTIVE, word );
                if ( w != null )
                    return w.getLemma().toString();
                w = morph.lookupBaseForm( POS.ADVERB, word );
                if ( w != null )
                    return w.getLemma().toString();
            }
            catch ( JWNLException e )
            {
            }
            return null;
        }

        /**
         * Stem a single word
         * tries to look up the word in the AllWords HashMap
         * If the word is not found it is stemmed with WordNet
         * and put into AllWords
         *
         * @param word word to be stemmed
         * @return stemmed word
         */
        public String Stem( String word )
        {
            // check if we already know the word
            String stemmedword = AllWords.get(word);
            if ( stemmedword != null )
                return stemmedword; // return it if we already know it

            // don't check words with digits in them
            // if ( containsNumbers (word) == true )
            //        stemmedword = null;
            //    else	// unknown word: try to stem it
            stemmedword = StemWordWithWordNet (word);

            if ( stemmedword != null )
            {
                // word was recognized and stemmed with wordnet:
                // add it to hashmap and return the stemmed word
                AllWords.put( word, stemmedword );
                return stemmedword;
            }
            // word could not be stemmed by wordnet,
            // thus it is no correct english word
            // just add it to the list of known words so
            // we won't have to look it up again
            AllWords.put( word, word );
            return word;
        }

    }
}
