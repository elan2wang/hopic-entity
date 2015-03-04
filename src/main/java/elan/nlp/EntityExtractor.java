package elan.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elan.nlp.http.Response;
import elan.nlp.util.ConfigUtil;
import elan.nlp.util.FileUtil;

public class EntityExtractor {
	private static final Logger logger = LoggerFactory.getLogger(EntityExtractor.class);
	
	private AlchemyAPI alchemyAPI;

	public EntityExtractor() {
		alchemyAPI = new AlchemyAPI(ConfigUtil.getValue("ALCHEMY_API"), ConfigUtil.getValue("ALCHEMY_API_KEY"));
	}

	public void batchProcess(String src, String dstPrefix) throws IOException {
		String all = dstPrefix + "all.txt";
		String err = dstPrefix + "err.txt";

		FileWriter all_fw = FileUtil.open(all);
		FileWriter err_fw = FileUtil.open(err);

		
		BufferedReader reader = new BufferedReader(new FileReader(src));
		String line = null;
		int num = 1;
		while ((line = reader.readLine()) != null) {
			String url = line.split("\t")[2];
			// send Alchemy API request
			Response response = alchemyAPI.getRankedNamedEntities(url);
			if (response == null) {
				FileUtil.append(err_fw, line);
				FileUtil.append(all_fw, "\n");
				System.out.print("W");
				logger.error("W");
				continue;
			}

			// get entities from result
			HashMap<String, Integer> entities = (HashMap<String, Integer>) getEntities(response);
			StringBuffer sb = new StringBuffer();
			for (Entry<String, Integer> entry : entities.entrySet()) {
				sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\t");
			}
			sb.append("\n");
			FileUtil.append(all_fw, sb.toString());
			System.out.print("|");
			if ((num++)%20 == 0) System.out.println();
		}
		reader.close();
		FileUtil.close(all_fw);
		FileUtil.close(err_fw);
	}

	private Map<String, Integer> getEntities(Response response) {
		HashMap<String, Integer> entityMap = new HashMap<String, Integer>();
		try {
			JSONArray entities = new JSONObject(response.asString()).getJSONArray("entities");
			for (int i=0; i<entities.length(); i++) {
				JSONObject entity = entities.getJSONObject(i);
				String type = entity.getString("type");
				if (type.equalsIgnoreCase("person")) {
					String entity_name = entity.getString("text");
					Integer entity_count = Integer.parseInt(entity.getString("count"));

					entityMap.put(entity_name, entity_count);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return entityMap;
	}

	public static void main(String args[]) throws IOException {
		EntityExtractor ee = new EntityExtractor();
		if (args.length != 2) {
			System.out.println("please indicate the input file and output file prefix");
			return;
		}
		System.out.println("extract entity started ...");
		
		String src_file = args[0];
		String dst_file = args[1];
		
		ee.batchProcess(src_file, dst_file);
		
		System.out.println("extract entity finished ...");
	}
}