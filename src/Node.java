import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Node extends SQLite{



	static String CREATE_NODE_TABLE = 
			"CREATE TABLE " +  "Nodes " + "(\r\n" + 
			"	_id INTEGER PRIMARY KEY,\r\n" + 
			"	bid INTEGER NOT NULL,\r\n" + 
			"	parentNode INTEGER,\r\n" + 
			"	nodeType INTEGER not null,\r\n" + 
			"	siblingNum INTEGER not null,\r\n" + //0 means the first sibling 
			"	nodeTitle TEXT,\r\n" + 
			
			//for support of altStructs
			"	isPrimaryStruct BOOLEAN NOT NULL default 1,\r\n" + 
			"	startTid INTEGER,\r\n" +  //maybe only used with refferences on alt structure
			"	endTid INTEGER,\r\n" +  //maybe only used with refferences on alt structure
			"	extraTids TEXT,\r\n" +  //maybe only used with refferences on alt structure ex. "[34-70,98-200]"
//maybe some stuff like to display chap name and or number (ei. maybe add some displaying info)
			
			"	FOREIGN KEY (bid) \r\n" + 
			"		REFERENCES Books (_id)\r\n" + 
			"		ON DELETE CASCADE,\r\n" + 
			"	FOREIGN KEY (parentNode) \r\n" + 
			"		REFERENCES Nodes (_id)\r\n" + 
			"		ON DELETE CASCADE,\r\n" + 
			"	CONSTRAINT uniqSiblingNum UNIQUE (parentNode,siblingNum)\r\n" + 

				")";
	


	private static void textDontDisplayNum(Connection c, String title){
		//Assuming that the whole book has the same rules (at least for this input method)
		String sql = "UPDATE Texts set displayNumber = 0 WHERE bid in (SELECT B._id FROM Books B WHERE B.title = ?);" ;
		PreparedStatement stmt = null;
		try {
			stmt = c.prepareStatement(sql);
			stmt.setString(1, title);
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	protected static int addText(Connection c, JSONObject json) throws JSONException{

		int lang = returnLangNums(json.getString("language"));
		String title = json.getString("title");
		Boolean isFirstLang = true;
		if(!booksInDB.containsKey(title)){
			System.err.println("Don't have book in DB and trying to add text");
			return -1;
		}
		int id = booksInDBbid.get(title);
		int textDepth = booksInDBtextDepth.get(title);
		int [] it = new int[MAX_LEVELS + 1];
		int forLoopNum = 6;
		if(forLoopNum != MAX_LEVELS)
			System.err.println("ERROR: forLoopNum is not teh same as MAX_LEVELS");
		boolean [] skipThisLoop = new boolean[forLoopNum + 1];
		JSONArray [] jsonArray = new JSONArray[forLoopNum + 1];

		jsonArray[forLoopNum] = (JSONArray) json.get("text");


		for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
			if(textDepth >= forLoopNum)
				try{
					jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
				}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
					continue;
				}
			else{
				jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
				skipThisLoop[forLoopNum] = true;
			}
			forLoopNum = 5;
			for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
				if(textDepth >= forLoopNum)
					try{
						jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
					}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
						continue;
					}
				else{
					jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
					skipThisLoop[forLoopNum] = true;
				}
				forLoopNum = 4;
				for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
					if(textDepth >= forLoopNum)
						try{
							jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
						}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
							continue;
						}
					else{
						jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
						skipThisLoop[forLoopNum] = true;
					}
					forLoopNum = 3;
					for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
						if(textDepth >= forLoopNum){
							try{
								jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
							}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
								continue;
							}

						}
						else{
							jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
							skipThisLoop[forLoopNum] = true;
						}
						forLoopNum = 2;
						for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
							if(textDepth >= forLoopNum){
								try{
									jsonArray[forLoopNum -1] = jsonArray[forLoopNum].getJSONArray(it[forLoopNum]);
								}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
									continue;
								}
							}
							else{
								jsonArray[forLoopNum - 1] = jsonArray[forLoopNum];
								skipThisLoop[forLoopNum] = true;
							}
							forLoopNum = 1;
							for(it[forLoopNum] = 0; !skipThisLoop[forLoopNum] && it[forLoopNum]<jsonArray[forLoopNum].length();it[forLoopNum]++){
								try{
									insertValues(c, title, textDepth, id, jsonArray[forLoopNum], lang, it, isFirstLang);
								}catch(Exception e){//MOST LIKELY THIS IS B/C THERE IS A 0 and not another level of JSON there.
									continue;
								}
							}
							forLoopNum = 2;
						}
						forLoopNum = 3;
					}
					forLoopNum = 4;
				}
				forLoopNum = 5;
			}
			forLoopNum = 6;
		}
		return 1; //it worked
	}



	private static int  insertValues(Connection c, String title,int textDepth, int id, JSONArray jsonLevel1, int lang, int [] it, boolean isFirstLang) throws JSONException{
		String theText;
		try{
			Object textObj = jsonLevel1.get(it[1]);
			if(!(jsonLevel1.get(it[1]) instanceof String)) //it's not text
				return -1;	
			theText = (String) textObj;
			if(theText.length() < 1){ //this means that it's useless to try to add to the database.
				return -1;
			}
			//theText = convertToJH8(theText);////CONVERT FROM UTF8!!!!!!!
			//convertFromJH8(theText);
		}catch(Exception e){ //if there was a problem getting the text, then it probably wasn't text anyways so just leave the function.
			System.err.println("Error: " + e);
			System.err.println("sql_adding_text: Problem adding text " + title + " it[1] = " + it[1]);
			textsFailedToUpload++;
			return -1;
		}


		PreparedStatement stmt = null;
		try{


			stmt = c.prepareStatement("INSERT INTO Texts ("
					+ Kbid + ", " + KenText + ", " + KheText + ", " 
					+ Klevel1 + ", " + Klevel2 + ", "+ Klevel3 + ", "+ Klevel4 + ", "+ Klevel5 + ", "+ Klevel6 + ")"
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");

			stmt.setInt(1,id); // Kbid
			int useableLang = lang + 1;//english =1 -> 2 & hebrew = 2 ->3
			//////////////////////

			stmt.setString(useableLang,theText); // KenText or  KheText




			final int LEVEL_IN_STATEMENT_START = 4;
			for(int i = 1; i<= MAX_LEVELS; i++){
				int num = 0;
				if(i<=textDepth)
					num = it[i]+1;
				stmt.setInt(LEVEL_IN_STATEMENT_START + i - 1,num);
			}
			stmt.executeUpdate();
			stmt.close();
			Searching.countWords(lang,theText, ++textsUploaded);

		}catch(SQLException e){
			if(e.getMessage().equals(HAS_TEXT_SQL_ERROR)){ //this text has already been placed into the db, so now you just want to add the text in the new lang.
				try{	
					int [] levels = new int [textDepth];
					for(int i = 1; i<= textDepth; i++){
						levels[i-1] = it[i] + 1;
					}
					String updateStatement = "UPDATE Texts set " + convertLangToLangText(lang) + " = ? WHERE " + whereClause(id, levels);
					stmt = c.prepareStatement(updateStatement);

					stmt.setString(1, theText);
					stmt.setInt(2, id); //bid
					final int LEVEL_IN_UPDATE_START = 3;
					for(int i =1; i<=textDepth; i++){
						stmt.setInt(LEVEL_IN_UPDATE_START + i - 1,it[i] + 1);
					}
					stmt.executeUpdate();
					stmt.close();
					///TODO currTID should be correct!!!

					if(lang == 2){
						String findTID = "SELECT _id FROM Texts WHERE " + whereClause(id, levels);
						stmt = c.prepareStatement(findTID);
						ResultSet rs;
						stmt.setInt(1, id); //bid
						final int LEVEL_IN_UPDATE_START_2 = 2;
						for(int i =1; i<=textDepth; i++){
							stmt.setInt(LEVEL_IN_UPDATE_START_2 + i - 1,it[i] + 1);
						}
						rs = stmt.executeQuery();
						int tid = -1;
						if ( rs.next() ) {
							tid = rs.getInt(1);
						}
						else
							System.err.println("couldn't find tid");
						Searching.countWords(lang,theText, tid);
					}

				}catch(Exception e1){
					System.err.println("ERROR: " + e1);
					textsFailedToUpload++;
				}
			}
			else{
				System.err.println("ERROR: " + e);
				textsFailedToUpload++;
			}
		}
		return 0;
	}


	private static String convertLangToLangText(String lang){
		if(lang.equals("en"))
			return KenText;
		else if(lang.equals("he"))
			return  KheText;
		System.err.println( "sql_text_convertLang: Unknown lang");
		return "";
	}

	protected static String convertLangToLangText(int lang){
		if(lang == 1)
			return KenText;
		else if(lang == 2)
			return  KheText;
		System.err.println( "sql_text_convertLang: Unknown lang");
		return "";
	}


	protected static String whereClause(int bid, int[] levels){
		String whereStatement = "bid = ? ";
		for(int i = 0; i < levels.length; i++){
			if(!(levels[i] == 0)){
				whereStatement += " AND level" + String.valueOf(i + 1) + "=? ";
			}
		}
		return whereStatement;
	}

	public static String bytesToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 3);

		for(int i =0 ; i< a.length; i++){
			int num = Byte.toUnsignedInt(a[i]);
			sb.append("0x" + Integer.toHexString(num) + " " );
		}
		return sb.toString();
	}


	static boolean isValidUTF8(final byte[] bytes) {
		try {
			Charset.availableCharsets().get("UTF-8").newDecoder().decode(ByteBuffer.wrap(bytes));
		} catch (CharacterCodingException e) {
			return false;
		}
		return true;
	}

	static final int  NEXT_CHAR_IS_UNICODE_INT =  0xFA;
	static final byte NEXT_CHAR_IS_UNICODE = (byte) NEXT_CHAR_IS_UNICODE_INT;

	static String convertToJH8(String original) throws UnsupportedEncodingException{
		ArrayList<Byte> newBytesList = new ArrayList <Byte> () ;  
		byte [] orgbytes = original.getBytes(Charset.forName("UTF-8"));
		//System.out.println(bytesToHex(orgbytes));
		if(!isValidUTF8(orgbytes)){
			System.err.println("ERROR: NOT VALID UTF-8");
			System.exit(-1);
		}
		for(int i = 0 ; i < orgbytes.length; i++){
			int num = Byte.toUnsignedInt(orgbytes[i]);
			int unicodeAmount = 0;
			if(num <= 0x7F){ //it's english
				newBytesList.add((byte) num);
			}
			else if(num == 0xD6 || num == 0xD7){//it's hebrew (unless it's armenian)
				int diff = 0;
				int nextNum = Byte.toUnsignedInt(orgbytes[++i]);
				if(num == 0xD7){
					diff = nextNum - 0x80 + 48; //48 is  (0xD6BF - 0xD690 + 1)
				}else{//num == 0xD6
					if(!(nextNum<= 0x90)){ 
						diff = nextNum - 0x90;
					}else{ //It's the rare few ARMENIAN chars from U+057F to U+058F
						diff = NEXT_CHAR_IS_UNICODE_INT;
						i--; //bring it back i b/c we assumed that this byte was for diff.
						unicodeAmount = 2;
					}
				}
				newBytesList.add((byte) diff);
			}
			else if(num <= 0xDF){//It's 2 Unicode Chars
				newBytesList.add(NEXT_CHAR_IS_UNICODE);
				unicodeAmount = 2;
			}
			else if(num <= 0xEF){//It's 3 Unicode Chars
				newBytesList.add(NEXT_CHAR_IS_UNICODE);
				unicodeAmount = 3;
			}
			else { //There's 4 Unicode Chars
				newBytesList.add(NEXT_CHAR_IS_UNICODE);
				unicodeAmount = 4;
			}

			//Add the unicode letters that belong
			for(int j = 0 ;j < unicodeAmount; j++){
				newBytesList.add((byte) Byte.toUnsignedInt(orgbytes[++i]));
			}
		}
		byte [] newbytes = new byte[newBytesList.size()];
		int bNum = 0;
		for (Byte b : newBytesList){
			newbytes[bNum++] = b;
		}

		String newString2 = new String(newbytes, "ISO-8859-1");
		/*
		byte [] testBytes = newString2.getBytes(Charset.forName("ISO-8859-1"));
		System.out.println(bytesToHex(testBytes));
		bNum = 0;
		for (Byte b : newBytesList){
			if(testBytes[bNum] != b){

				System.err.print("YOU SUCK: 0x" + Integer.toHexString( Byte.toUnsignedInt(b)) + " 0x" + Integer.toHexString(Byte.toUnsignedInt(testBytes[bNum])));
				System.exit(-1);
			}
			else
				System.out.print(".");
		}
		 */

		return newString2;
	}


	static String convertFromJH8(String jhString) throws UnsupportedEncodingException{
		ArrayList<Byte> newBytesList = new ArrayList <Byte> () ;  
		byte [] jhBytes = jhString.getBytes(Charset.forName("ISO-8859-1"));
		for(int i = 0 ; i < jhBytes.length; i++){
			int num = Byte.toUnsignedInt(jhBytes[i]);
			//int unicodeAmount = 0;
			if(num == NEXT_CHAR_IS_UNICODE_INT){
				int nextNum = Byte.toUnsignedInt(jhBytes[i+1]);
				int unicodeAmount = 0;
				if(nextNum <= 0xDF){//It's 2 Unicode Chars
					unicodeAmount = 2;
				}
				else if(nextNum <= 0xEF){//It's 3 Unicode Chars
					unicodeAmount = 3;
				}
				else { //There's 4 Unicode Chars
					unicodeAmount = 4;
				}
				for(int j = 0; j< unicodeAmount;j++){
					newBytesList.add(jhBytes[++i]);
				}
			}
			else if(num <= 0x7F){ //it's english
				newBytesList.add((byte) num);
			}
			else //if(num >= 0x80){//it's hebrew
				if(num < 176){//original unicode was 0xD6 //176 = 48 + 0x80
					newBytesList.add((byte) 0xD6);
					newBytesList.add((byte)(0x10 + num));//0x10 = 0x90 - 0x80
				}
				else{ //original unicode was 0xD7)
					newBytesList.add((byte) 0xD7);					
					newBytesList.add((byte)(num - 48)); 
				}
		}
		//else //there's an error

		byte [] newbytes = new byte[newBytesList.size()];
		int bNum = 0;
		for (Byte b : newBytesList){
			newbytes[bNum++] = b;
		}

		String newString2 = new String(newbytes, "UTF-8");
		//System.out.println("FINISH: " + newString2);

		byte [] testBytes = newString2.getBytes(Charset.forName("ISO-8859-1"));
		System.out.print("P: " + newString2);
		System.out.print("P: "); 
		System.out.println(bytesToHex(testBytes));

		return newString2;
	}








}
