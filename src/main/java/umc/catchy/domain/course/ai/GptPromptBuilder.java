package umc.catchy.domain.course.ai;

import org.springframework.stereotype.Component;
import umc.catchy.domain.place.domain.Place;

import java.util.List;

@Component
public class GptPromptBuilder {

    private static final String KEY_COURSE_NAME = "courseName";
    private static final String KEY_COURSE_DESC = "courseDescription";
    private static final String KEY_RECOMMEND_TIME = "recommendTime";
    private static final String KEY_PLACES = "places";
    private static final String KEY_PLACE_ID = "placeId";
    private static final String KEY_PLACE_NAME = "name";
    private static final String KEY_PLACE_ADDRESS = "roadAddress";
    private static final String KEY_PLACE_HOURS = "operatingHours";

    public String buildCourseRecommendationPrompt(
            List<String> regions,
            List<Place> places,
            List<String> preferredCategories,
            List<String> userStyles,
            List<String> activeTimes
    ) {
        StringBuilder prompt = new StringBuilder();

        appendIntroAndRegions(prompt, regions);
        appendUserPreferences(prompt, preferredCategories, userStyles, activeTimes);
        appendPlaceCandidates(prompt, places);
        appendJsonOutputFormat(prompt);

        return prompt.toString();
    }

    private void appendIntroAndRegions(StringBuilder prompt, List<String> regions) {
        String introTemplate = """
                Create a unique and creative itinerary for the following regions: %s.
                Randomly select **2 to 5** unique places from the list below to create a diverse and interesting itinerary.
                Do not include all places in the itinerary.
                """;
        prompt.append(introTemplate.formatted(String.join(", ", regions)));
    }

    private void appendUserPreferences(StringBuilder prompt, List<String> preferredCategories, List<String> userStyles, List<String> activeTimes) {
        prompt.append("The user's preferred categories are: ").append(String.join(", ", preferredCategories)).append(".\n");

        if (!userStyles.isEmpty()) {
            prompt.append("The user prefers the following styles: ").append(String.join(", ", userStyles)).append(".\n");
        }

        if (!activeTimes.isEmpty()) {
            prompt.append("The user's preferred active times are: ").append(String.join(", ", activeTimes)).append(".\n");
        }
    }

    private void appendPlaceCandidates(StringBuilder prompt, List<Place> places) {
        prompt.append("Here are the places to choose from:\n");
        for (Place place : places) {
            prompt.append(String.format(
                    "- Place ID: %d, Name: %s, Road Address: %s, Operating Hours: %s, Category: %s, Description: %s\n",
                    place.getId(), place.getPlaceName(), place.getRoadAddress(), place.getActiveTime(), place.getCategory().getName(), place.getPlaceDescription()
            ));
        }
    }

    private void appendJsonOutputFormat(StringBuilder prompt) {
        String jsonInstructions = """
                
                The course name and description must be written in Korean.
                The course description should be concise, no more than 80 characters.
                The response should include a course name, course description, recommended visit time.
                Please return only the JSON structure below without any additional text, comments, or markdown formatting (e.g., no ```json). Return only the raw JSON structure:
                """;

        String jsonStructure = """
                {
                  "%s": "string (in Korean)",
                  "%s": "string (in Korean)",
                  "%s": "HH:mm~HH:mm",
                  "%s": [
                    {
                      "%s": "numeric",
                      "%s": "string",
                      "%s": "string",
                      "%s": "HH:mm-HH:mm"
                    }
                  ]
                }
                """;

        prompt.append(jsonInstructions);
        prompt.append(jsonStructure.formatted(
                KEY_COURSE_NAME,
                KEY_COURSE_DESC,
                KEY_RECOMMEND_TIME,
                KEY_PLACES,
                KEY_PLACE_ID,
                KEY_PLACE_NAME,
                KEY_PLACE_ADDRESS,
                KEY_PLACE_HOURS
        ));
    }
}
