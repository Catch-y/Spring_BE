package umc.catchy.infra.osrm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import umc.catchy.domain.course.dto.request.CourseOSRMRequest;

@Service
public class OsrmService {

    @Value("${osrm.base-url}")
    private String osrmBaseUrl;
    private final RestTemplate restTemplate;

    public OsrmService(){
        this.restTemplate = new RestTemplate();
    }

    public OsrmResponse getRoute(CourseOSRMRequest.courseInfo request){
        String start = request.getStart().getLongitude() + "," + request.getStart().getLatitude();
        String end = request.getEnd().getLongitude() + "," + request.getEnd().getLatitude();

        StringBuilder osrmUrl = new StringBuilder(osrmBaseUrl + start + ";");
        //경유지 설정
        for(CourseOSRMRequest.routeInfo route : request.getRoutes()){
            String routeUrl = route.getLongitude() + "," + route.getLatitude() + ";";
            osrmUrl.append(routeUrl);
        }
        osrmUrl.append(end).append("?steps=true");
        return restTemplate.getForObject(osrmUrl.toString(), OsrmResponse.class);
    }
}
