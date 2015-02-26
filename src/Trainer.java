import java.io.BufferedWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by Nisansa on 15/02/26.
 */
public class Trainer {
    HashSet<String> unigrams = new HashSet<String>();
    HashSet<String> bigrams = new HashSet<String>();
    HashSet<String> hosts = new HashSet<String>();
    ArrayList<Phrase> phrases = new ArrayList<Phrase>();

    public static void main(String[] args) {
        Trainer c = new Trainer();
        String path = "./";
        c.readDataFile(path + "train.tsv", false);
       // c.readDataFile(path + "Spam.txt", true);
       // c.writeFile(path);
    }

    public void writeFile(String path) {
        String line = "@relation spam\n";
        Iterator<String> itr = unigrams.iterator();
        while (itr.hasNext()) {
            line += "@attribute 'S_" + itr.next() + "' { t}\n";
        }
        itr = bigrams.iterator();
        while (itr.hasNext()) {
            line += "@attribute 'S_" + itr.next() + "' { t}\n";
        }
        itr = unigrams.iterator();
        while (itr.hasNext()) {
            line += "@attribute 'B_" + itr.next() + "' { t}\n";
        }
        itr = bigrams.iterator();
        while (itr.hasNext()) {
            line += "@attribute 'B_" + itr.next() + "' { t}\n";
        }
        line += "@attribute 'Host' {";
        itr = hosts.iterator();
        while (itr.hasNext()) {
            line += "'" + itr.next() + "',";
        }
        line = line.substring(0, line.length() - 1); // Drop the last commma
        line += "}\n";
        line += "@attribute 'Class' {'Spam','Ham'}\n@data\n";
        for (int i = 0; i < phrases.size(); i++) {
            Phrase m = phrases.get(i);
            line += m.toARFFstring(unigrams, bigrams);
        }


        try {
            File statText = new File(path+"/SpamHam.arff");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            BufferedWriter w = new BufferedWriter(osw);
            w.write(line);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file SpamHam.arff");
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
               // System.out.println(line);
                String[] parts=line.split("\t");
                Phrase p=new Phrase(parts);
                System.out.println(p.toString());
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
            String line=phraseStat.toARFFstring(unigrams,bigrams)+","+sentiment;//+",";
      /*      if(isSpam){
                line+="Spam";
            }
            else{
                line+="Ham";
            }*/
            line+="\n";
            return(line);
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
            totalLength=parts.length;
            for(int i=0;i<parts.length;i++){
                unigrams.add(parts[i]);
                if(i>0){
                    bigrams.add(parts[i-1]+" "+parts[i]);
                }
            }

        }

        public String toARFFstring(HashSet<String> allUnigrams,HashSet<String> allBigrams){
            String line="";
            Iterator<String> itr=allUnigrams.iterator();
            while(itr.hasNext()){
                if(unigrams.contains(itr.next())){
                    line+="t,";
                }
                else{
                    line+="?,";
                }
            }
            itr=allBigrams.iterator();
            while(itr.hasNext()){
                if(bigrams.contains(itr.next())){
                    line+="t,";
                }
                else{
                    line+="?,";
                }
            }
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

}
