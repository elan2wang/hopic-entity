package elan.nlp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Session {
	Integer id;
	Integer start;
	Integer duration;
	List<Integer> members;
	
	public Session(Integer id, Integer start,Integer duration,List<Integer> members) {
		this.id = id;
		this.start = start;
		this.duration = duration;
		this.members = members;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(id + ", " + start + ", " + duration + ",  [ ");
		for (Integer mem : members) sb.append(mem + " ");
		sb.append("]");
		return sb.toString();
	}
}

public class EntityStatistic {
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	public static void main(String args[]) {
		HashMap<Integer, List<Integer>> map = entity_doc_map("News/entity/entity_all.txt");
		HashMap<Integer, Integer> dateMap = date_doc_map("News/todolist/Guardian_Edward_Snowden.dat");
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader("News/entity/top-id.txt"));
			
			List<List<Integer>> entityDates = new ArrayList<List<Integer>>();
			String line = null;
			while ((line = reader.readLine()) != null) {
				List<Integer> list = null;
				if (line.contains(" ")) {
					String[] ids = line.split(" ");
					list = new ArrayList<Integer>();
					for (String id : ids) {
						List<Integer> tmp = map.get(Integer.parseInt(id));
						for (Integer docid : tmp) {
							if (!list.contains(docid)) list.add(docid);
						}
					}
				} else {
					list = map.get(Integer.parseInt(line.trim()));
				}
				
				// print
				Collections.sort(list);
				
				List<Integer> dates = new ArrayList<Integer>();
				for (Integer docid : list) {
					Integer interval = dateMap.get(docid);
					if (!dates.contains(interval)) {
						dates.add(interval);
					}
				}
				entityDates.add(dates);
			}
			////////////////////
			List<Session> sessions = generateSession(entityDates, 7);
			
			// output json
			JSONObject json = new JSONObject();
			json.put("panels", 220);
			
			JSONArray jarray = new JSONArray();
			for (Session session : sessions) {
				JSONObject obj = new JSONObject();
				obj.put("id", session.id);
				obj.put("start", session.start);
				obj.put("duration", session.duration);
				obj.put("chars", session.members);
				jarray.put(obj);
			}
			
			json.put("sessions", jarray);
			
			System.out.println(json.toString());
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	private static boolean dateInDuration(List<Integer> dates, int session, int duration) {
		for(Integer date : dates) {
			if(date >= session * duration && date < (session + 1) * duration) {
				return true;
			}
		}
		return false;
	}

	private static List<Session> generateSession(List<List<Integer>> entityDates, int duration) {
		List<Session> sessions = new ArrayList<Session>();
		for(int session=0; session<250/duration; session++) {
			List<Integer> members = new ArrayList<Integer>();
			for(int i=1; i<=entityDates.size(); i++) {
				// date from : (session ~ session + 1) * duration
				List<Integer> dates = entityDates.get(i-1);
				if(dateInDuration(dates, session, duration)) members.add(i);
			}
			if(members.size() > 1) {
				Session s = new Session(sessions.size(), session * duration, duration, members);
				sessions.add(s);
			}
		}

		return sessions;
	}

	/**
	 * 实体编号 ～ 文档列表
	 *
	 * 
	 * @param filename
	 * @return
	 */
	public static HashMap<Integer, List<Integer>> entity_doc_map(String filename) {
		HashMap<Integer, List<Integer>> res = new HashMap<Integer, List<Integer>>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = null;
			Integer line_num = 1552;
			
			while ((line = reader.readLine()) != null) {
				if (!line.trim().equals("")) {
					String entries[] = line.split("\t");
					for (String entry : entries) {
						Integer id = Integer.parseInt(entry.split(":")[0]);
						if (res.containsKey(id)) {
							res.get(id).add(line_num);
						} else {
							List<Integer> list = new ArrayList<Integer>();
							list.add(line_num);
							res.put(id, list);
						}
					}
				}
				line_num--;
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return res;
	}

	/**
	 * 文档编号 ～ 日期编号
	 * 
	 * @param filename
	 * @return
	 */
	public static HashMap<Integer, Integer> date_doc_map(String filename) {
		HashMap<Integer, Integer> res = new HashMap<Integer, Integer>();
		Calendar begin_date = Calendar.getInstance();
		try {
			begin_date.setTime(sdf.parse("2013-06-09"));
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = null;
			Integer line_num = 1552;
			
			while ((line = reader.readLine()) != null) {
				String date = line.substring(0, 10);
				Calendar cur_date = Calendar.getInstance();
				try {
					cur_date.setTime(sdf.parse(date));
				} catch (ParseException e) {
					e.printStackTrace();
				}
				Integer interval = cur_date.get(Calendar.DAY_OF_YEAR) - begin_date.get(Calendar.DAY_OF_YEAR);
				if (interval < 0) {
					interval += begin_date.getActualMaximum(Calendar.DAY_OF_YEAR);
				}
				
				res.put(line_num, interval);
				
				line_num--;
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return res;
	}
}
