package edu.usc.cyberpolar.scraper;

import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import java.net.URL;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.Link;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

public class AcadisMimeScraper

{
    private Map<String, Integer> formatCounts = new HashMap<String, Integer>();  

    public void scrape(Map<String, String> replaceGroup, boolean trim, boolean lowerCase){
        try{
          //Base ULR to search  
            String urlBase = "https://www.aoncadis.org/";
          
	      //URL of with search parameters		
			URL url = new URL(urlBase+"scienceKeywordTopic/Atmosphere.html");

		  //Tika Parser
			Parser parser = new AutoDetectParser();
			
		  //Content handler that collects links from an XHTML document.
			LinkContentHandler handler = new LinkContentHandler();
		 
		  //Constructs a new, empty metadata.
			Metadata met = new Metadata();
			
		  //ParseContext=Used to pass context information to Tika parsers.
            parser.parse(url.openStream(), handler, met, new ParseContext());

		  //getLinks returns the list of collected links.
			for(Link link: handler.getLinks()){
			
		      //A URI is a Uniform Resource Identifier (URI), in this case, we want URIs that start with the word Metadata	
				if(link.getUri().startsWith("/dataset")){
				  //nextURL = url with the word metadata	
					String nextUrl = urlBase+link.getUri();
				    //System.out.println(nextUrl); //this will print a list of all the metadata urls
			      
			      //Content handler decorator that only passes everything inside the XHTML <body/> tag to the underlying handler.		
					BodyContentHandler bodyHandler = new BodyContentHandler(10*1024*1024);
					
				  //Opens a connection to nextURL and returns an InputStream for reading from that connection, where bodyHandler only passes what is inside the XHTML
				  //to the handler. Then this gets passed to Tika parsers.	
					parser.parse(new URL(nextUrl).openStream(), bodyHandler, met, new ParseContext());
				
				  //Gives string representation of what is within the XHTML
					String content = bodyHandler.toString();

				  //For this script to find the file format, the "Data Format" has to be set in the URL.
				  //i.e. Data Format(s): JPEG
					String regex = "Data Format\\(s\\):\\s*([\\w, ?]*)";
				  
				  /*A regular expression, specified as a string, must first be compiled into an instance of this class. 
				    The resulting pattern can then be used to create a Matcher object that can match arbitrary character 
				    sequences against the regular expression. All of the state involved in performing a match resides in the matcher,
				    so many matchers can share the same pattern.*/				
					Pattern pattern = Pattern.compile(regex);
					
			      //An engine that performs match operations on a character sequence by interpreting a Pattern.		
					Matcher matcher = pattern.matcher(content);

					List<String> formats = new Vector<String>();
					
				  //The find method scans the input sequence looking for the next subsequence that matches the pattern.
					if (matcher.find()){
						formats = Arrays.asList(matcher.group(1).split(","));
						formats = cleanse(formats, replaceGroup, trim, lowerCase);
					}
					else{
					  //Returns 'unknown' if no match	
						formats.add("unknown");
					}
				
				  //This bit of code counts the number of each file type found
					for(String format: formats){
						if(formatCounts.containsKey(format)){
							int count = formatCounts.get(format);
							count++;
							formatCounts.put(format, count);
						}
						else{
							formatCounts.put(format, 1);
						}
					}
				}
			}

		  //This bit of code prints the different formats and their #s
			StringBuffer keyBuf = new StringBuffer();
			for(String key: formatCounts.keySet()){
				keyBuf.append(key);
				keyBuf.append(",");
			}
			System.out.println(keyBuf.toString());
	    
			StringBuffer valBuf = new StringBuffer();
			for(String key: formatCounts.keySet()){	    
				valBuf.append(formatCounts.get(key));
				valBuf.append(",");
			}
			System.out.println(valBuf.toString());

		}catch(Exception e){
			e.printStackTrace();
			return;
		}
    }

  //Entering the cleanse
    public List<String> cleanse(List<String> formats, Map<String, String> replaceGroup, boolean trim, boolean lowerCase){
		List<String> cleansedFormats = new Vector<String>(formats.size());
		for(String format: formats){
			String cleansedFormat = format;
		  
		  //if 'trim' is true, return string with leading and trailing whitespace removed	
			if(trim) cleansedFormat = cleansedFormat.trim();
		  
		  //if 'lower case' is true, return string with all characters in lower case	
		    if(lowerCase) cleansedFormat = cleansedFormat.toLowerCase();
			boolean dontAdd=false;

		  //This bit looks for the replacement strings, i.e. Portable document format to PDF
			for(String groupKey: replaceGroup.keySet()){
				if(cleansedFormat.indexOf(groupKey) != -1){
					cleansedFormats.add(replaceGroup.get(groupKey));
					dontAdd=true;
				}
			}

			if (!dontAdd) cleansedFormats.add(cleansedFormat);
		}
	  //returns files after trim and lower case applied	
		return cleansedFormats;
    }

    //Main Class
    public static void main(String [] args) throws Exception{
	  //Print input arguments
		System.out.println(Arrays.toString(args)); 	
	 
	  //Example of command line syntax
		String usage = "java -classpath .:tika-app-1.5.jar AcadisMimeScraper <replace|with:replace|with..> <trim> <lower case>>\n";
 
      //Setting up scraper to search URLs for file types
		AcadisMimeScraper scraper = new AcadisMimeScraper();
	  
	  //If there are less than 3 arguments entered at the command line, exit.
		if(args.length != 3){
			System.err.println(usage); // prints the example command line 'usage'
			System.exit(1);
		}
		
	  //replaceGroupString = whatever replacements were put in argument 1, ex:"Portable\ Document\ Format|PDF"
		String replaceGroupString = args[0];
		Map<String, String> replaceGroup = new HashMap<String, String>();
		String[] groups = replaceGroupString.split("\\:");
		
		for(int i=0; i < groups.length; i++){
			String[] groupSpec = groups[i].split("\\|");
			replaceGroup.put(groupSpec[0], groupSpec[1]);
		}
		
		boolean trim = Boolean.parseBoolean(args[1]);
		boolean lowerCase = Boolean.parseBoolean(args[2]);
        scraper.scrape(replaceGroup, trim, lowerCase);
    }
}
