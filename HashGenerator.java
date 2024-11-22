import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class HashGenerator {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar test.jar <roll_number> <json_file_path>");
            return;
        }

        String rollNumber = args[0].toLowerCase().replace(" ", ""); // Convert to lowercase and remove spaces
        String jsonFilePath = args[1];

        try {
            // Step 1: Read the JSON file content
            String jsonContent = readFile(jsonFilePath);

            // Step 2: Parse JSON and find the "destination" key
            Map<String, Object> jsonMap = parseJson(jsonContent);
            String destinationValue = findDestination(jsonMap);

            if (destinationValue == null) {
                System.out.println("Key 'destination' not found in the JSON file.");
                return;
            }

            // Step 3: Generate a random 8-character alphanumeric string
            String randomString = generateRandomString(8);

            // Step 4: Concatenate roll number, destination value, and random string
            String concatenatedString = rollNumber + destinationValue + randomString;

            // Step 5: Generate MD5 hash
            String md5Hash = generateMD5Hash(concatenatedString);

            // Step 6: Print the result
            System.out.println(md5Hash + ";" + randomString);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Recursive method to find the first instance of the "destination" key
    private static String findDestination(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equals("destination")) {
                return value.toString();
            } else if (value instanceof Map) {
                String result = findDestination((Map<String, Object>) value);
                if (result != null) {
                    return result;
                }
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        String result = findDestination((Map<String, Object>) item);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    // Basic JSON parsing: converts JSON to a Map<String, Object>
    private static Map<String, Object> parseJson(String json) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        Stack<Object> stack = new Stack<>();
        String key = null;

        char[] chars = json.toCharArray();
        StringBuilder buffer = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (inQuotes) {
                buffer.append(c);
            } else if (c == '{') {
                Map<String, Object> newMap = new LinkedHashMap<>();
                if (!stack.isEmpty() && stack.peek() instanceof List) {
                    ((List<Object>) stack.peek()).add(newMap);
                } else if (key != null) {
                    map.put(key, newMap);
                }
                stack.push(newMap);
                key = null;
            } else if (c == '[') {
                List<Object> newList = new ArrayList<>();
                if (!stack.isEmpty() && stack.peek() instanceof Map) {
                    ((Map<String, Object>) stack.peek()).put(key, newList);
                }
                stack.push(newList);
                key = null;
            } else if (c == '}' || c == ']') {
                stack.pop();
            } else if (c == ':') {
                key = buffer.toString().trim();
                buffer.setLength(0);
            } else if (c == ',' || c == '\n') {
                if (buffer.length() > 0) {
                    Object value = parseValue(buffer.toString().trim());
                    if (!stack.isEmpty() && stack.peek() instanceof Map && key != null) {
                        ((Map<String, Object>) stack.peek()).put(key, value);
                    } else if (!stack.isEmpty() && stack.peek() instanceof List) {
                        ((List<Object>) stack.peek()).add(value);
                    }
                    buffer.setLength(0);
                    key = null;
                }
            } else if (!Character.isWhitespace(c)) {
                buffer.append(c);
            }
        }
        return map;
    }

    private static Object parseValue(String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else if (value.equalsIgnoreCase("null")) {
            return null;
        } else if (value.matches("-?\\d+(\\.\\d+)?")) {
            return value.contains(".") ? Double.parseDouble(value) : Integer.parseInt(value);
        } else {
            return value;
        }
    }

    // Generate a random 8-character alphanumeric string
    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder randomString = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            randomString.append(characters.charAt(random.nextInt(characters.length())));
        }
        return randomString.toString();
    }

    // Generate an MD5 hash
    private static String generateMD5Hash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(input.getBytes());
        StringBuilder hashString = new StringBuilder();
        for (byte b : hashBytes) {
            hashString.append(String.format("%02x", b));
        }
        return hashString.toString();
    }

    // Read file content into a string
    private static String readFile(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
        }
        return contentBuilder.toString();
    }
}
