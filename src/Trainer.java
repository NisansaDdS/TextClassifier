import java.io.BufferedWriter;
import java.io.*;
import java.util.*;

import net.didion.jwnl.*;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.data.list.PointerTargetTreeNode;
import net.didion.jwnl.data.list.PointerTargetTreeNodeList;
import net.didion.jwnl.dictionary.*;
import net.didion.jwnl.dictionary.Dictionary;

/**
 * Created by Nisansa on 15/02/26.
 */
public class Trainer {
    HashSet<String> unigrams = new HashSet<String>();
    HashSet<String> bigrams = new HashSet<String>();
    HashSet<String> hosts = new HashSet<String>();
    ArrayList<Phrase> phrases = new ArrayList<Phrase>();
    ArrayList<Phrase> sentenses = new ArrayList<Phrase>();
    Stemmer stemmer=null;

    public static void main(String[] args) {


                 Trainer c = new Trainer();
                String path = "./";
                  c.readDataFile(path + "train.tsv", false);
                   c.writeFile(path);
    }


    public Trainer() {
        stemmer = new Stemmer();
        System.out.println(stemmer.Stem("cars"));
        System.out.println(stemmer.getBaseWord("Car"));

    }

    public void writeFile(String path) {
        StringBuilder s=new StringBuilder();

        //Sampling the training data
        ArrayList<Phrase> sentensesSample = new ArrayList<Phrase>();
        unigrams = new HashSet<String>();
        bigrams = new HashSet<String>();
        Random r=new Random();
      /*  for (int i = 0; i < (4*sentenses.size())/10; i++) {
            sentensesSample.add(sentenses.get(r.nextInt(sentenses.size()-1)));
        }*/
        sentensesSample.addAll(sentenses);
        for (int i = 0; i < sentensesSample.size(); i++) {
            unigrams.addAll(sentensesSample.get(i).getUnigrams());
            bigrams.addAll(sentensesSample.get(i).getBigrams());
        }



        s.append("@relation sentiment\n");
        Iterator<String> itr = unigrams.iterator();
        System.out.println(unigrams.size());
        while (itr.hasNext()) {
            s.append("@attribute 'S_");
            s.append(itr.next());
            s.append("' { t}\n");
        }
 /*       System.out.println(bigrams.size());
        itr = bigrams.iterator();
        while (itr.hasNext()) {
            s.append("@attribute 'S_");
            s.append(itr.next());
            s.append("' { t}\n");
        }*/

     /*   line += "@attribute 'Host' {";
        itr = hosts.iterator();
        while (itr.hasNext()) {
            line += "'" + itr.next() + "',";
        }
        line = line.substring(0, line.length() - 1); // Drop the last commma
        line += "}\n"; */
        s.append("@attribute 'Class' {'0','1','2','3','4'}\n@data\n");

        String line=s.toString();
        String fileName="train.arff";

        try {
            File statText = new File(path+"/"+fileName);
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            BufferedWriter w = new BufferedWriter(osw);
            w.write(line);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing headders to the file "+fileName);
        }

        line="";
        s=new StringBuilder();
        System.gc();

        System.out.println(sentenses.size());
        System.out.println(sentensesSample.size());
        for (int i = 0; i < sentensesSample.size(); i++) {
            Phrase m = sentensesSample.get(i);
            s.append(m.toARFFstring(unigrams, bigrams));

            if(i%100==0){
                line=s.toString();



                try {
                    File statText = new File(path+"/"+fileName);
                    FileWriter fw=new FileWriter(statText,true);
                   // FileOutputStream is = new FileOutputStream(statText);
                   // OutputStreamWriter osw = new OutputStreamWriter(is);
                    BufferedWriter w = new BufferedWriter(fw);
                    w.write(line);
                    w.close();
                } catch (IOException e) {
                    System.err.println("Problem writing lines to the file "+fileName);
                }




                line="";
                s=new StringBuilder();
                System.gc();
            }

        }






    }



    public void readDataFile(String path, Boolean isSpam) {
        String line=null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            line = br.readLine();
            line = br.readLine(); //Ignoring the first line
            int messagePartIndex = 0;
            StringBuilder sb = new StringBuilder();
            Phrase m = null;
            while (line != null) {
              //  System.out.println(line);
                String[] parts=line.split("\t");
                m=new Phrase(parts);

                //unigrams.addAll(m.getUnigrams());
                //bigrams.addAll(m.getBigrams());
                phrases.add(m);
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

        //Grab the longest of sentences
        int pOldIndex=0;
        for (int i = 1; i <phrases.size() ; i++) {
            Phrase pNew=phrases.get(i);
            Phrase pOld=phrases.get(pOldIndex);
            if(pOld.sentenceId!=pNew.sentenceId){
                sentenses.add(pOld);
                pOldIndex=i;
            }
            else{
                if(pOld.getUnigrams().size()<pNew.getUnigrams().size()){
                    pOldIndex=i;
                }
            }
        }

    }


    public class Phrase {

        int phraseId=0;
        int sentenceId=0;
        StringStat phraseStat;
        int sentiment=0;

        public Phrase(String[] line){
            this(line[0], line[1], line[2],line[3]);
        }

        public Phrase(String phraseIdS, String	sentenceIdS, String	phraseS,String	sentimentS){

            phraseId=Integer.parseInt(phraseIdS);
            sentenceId=Integer.parseInt(sentenceIdS);

            phraseStat=new StringStat(phraseS);
            sentiment=Integer.parseInt(sentimentS);


        }


        public String toString(){
            String line="PhraseId: "+phraseId+"\nSentenceId: "+sentenceId+"\n";
            line+="Phrase::\n"+phraseStat.toString()+"\nSentiment: "+sentiment+"\n";
            return line;
        }


        public HashSet<String> getUnigrams(){
            return phraseStat.getUnigrams();
        }

        public HashSet<String> getBigrams(){
            return phraseStat.getBigrams();
        }

        public String toARFFstring(HashSet<String> unigrams,HashSet<String> bigrams){
            StringBuilder s=new StringBuilder();
            s.append(phraseStat.toARFFstring(unigrams,bigrams));
            s.append(",");
            s.append(sentiment);
            s.append("\n");
      /*      if(isSpam){
                line+="Spam";
            }
            else{
                line+="Ham";
            }*/

            return(s.toString());
        }
    }

    public class StringStat {
        HashSet<String>  unigrams=new HashSet<String>();
        HashSet<String>  bigrams=new HashSet<String>();
        String original="";
        private int totalLength=0;
        //Source http://norm.al/2009/04/14/list-of-english-stop-words/
        String[] stopWords=new String[]{ "a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"};

        public StringStat(String s){
            original=s;
            s=s.replaceAll("…","...");
            s=s.replaceAll("([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\|\\)\\]])"," ");
            s=s.replaceAll(","," ");
            s=s.replaceAll("_"," ");
            s=s.replaceAll("-"," ");
            s=s.replaceAll(":"," ");
            s=s.replaceAll(";"," ");
            s=s.replaceAll("!"," ");
            s=s.replaceAll("/"," ");
            s=s.replaceAll("\""," ");
            s=s.replaceAll("“"," ");
            s=s.replaceAll("”"," ");
            s=s.replaceAll("="," ");
            s=s.replaceAll("'"," ");
            s=s.replaceAll("%"," %");

           // s=s.replaceAll("U$","US$");
            //System.out.println(s);
            s=s.replaceAll("( )+"," ");


            String[] parts=s.split(" ");

            ArrayList<String> stemmedParts=new ArrayList<String>();
            for (int i = 0; i <parts.length ; i++) {
               // System.out.println(parts[i]);
                String wordRes=stemmer.Stem(parts[i]);
                if(wordRes!=null){
                    stemmedParts.add(wordRes);
                }
                //stemmedParts[i]=stemmer.Stem(parts[i]);
               // System.out.println(stemmedParts[i]+" -> "+stemmer.getBaseWord(stemmedParts[i]));
            }
            parts=new String[stemmedParts.size()];
            for (int i = 0; i < stemmedParts.size(); i++) {
                parts[i]=stemmedParts.get(i);
            }


            totalLength=parts.length;
            for(int i=0;i<parts.length;i++){
                unigrams.add(parts[i]);
                if(i>0){
                    bigrams.add(parts[i-1]+" "+parts[i]);
                }
            }

        }

        public String toARFFstring(HashSet<String> allUnigrams,HashSet<String> allBigrams){
            StringBuilder s=new StringBuilder();

            Iterator<String> itr=allUnigrams.iterator();
            while(itr.hasNext()){
                if(unigrams.contains(itr.next())){
                    s.append("t,");
                }
                else{
                    s.append("?,");
                }
            }
   /*         itr=allBigrams.iterator();
            while(itr.hasNext()){
                if(bigrams.contains(itr.next())){
                    s.append("t,");
                }
                else{
                    s.append("?,");
                }
            }*/
            String line=s.toString();
            line=line.substring(0, line.length()-1); //Drop the last commma
            return line;
        }

        public HashSet<String> getUnigrams(){
            return unigrams;
        }

        public HashSet<String> getBigrams(){
            return bigrams;
        }

        int getTotalLength() {
            return totalLength;
        }


        public String toString(){
            String line="Word Count: "+getTotalLength()+"\n\nUnigrams\n";
            Iterator<String> itr=unigrams.iterator();
            while(itr.hasNext()){
                line+=itr.next()+"\n";
            }
            line+="\nBigrams\n";
            itr=bigrams.iterator();
            while(itr.hasNext()){
                line+=itr.next()+"\n";
            }
            return(line);
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


        public String getBaseWord(String sWord){
            int depth=0;
            dictionary = Dictionary.getInstance();
            p = PointerUtils.getInstance();

            try {
                IndexWord word = dictionary.lookupIndexWord(POS.VERB, sWord);
                if(word==null){
                    word = dictionary.lookupIndexWord(POS.NOUN, sWord);
                    depth=8;
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
