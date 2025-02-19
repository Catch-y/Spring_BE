package umc.catchy.infra.osrm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OsrmService {

    @Value("${osrm.base-url}")
    private String osrmBaseUrl;
    @Value("${osrm.param}")
    private String osrmOption;
    private final RestTemplate restTemplate;

    public OsrmService(){
        this.restTemplate = new RestTemplate();
    }

    public OsrmResponse getRoute(OsrmRequest.courseInfo request){
        String start = request.getStart().getLongitude() + "," + request.getStart().getLatitude();
        String end = request.getEnd().getLongitude() + "," + request.getEnd().getLatitude();

        StringBuilder osrmUrl = new StringBuilder(osrmBaseUrl + start + ";");
        //경유지 설정
        for(OsrmRequest.routeInfo route : request.getRoutes()){
            String routeUrl = route.getLongitude() + "," + route.getLatitude() + ";";
            osrmUrl.append(routeUrl);
        }
        osrmUrl.append(end).append(osrmOption);
        return restTemplate.getForObject(osrmUrl.toString(), OsrmResponse.class);
    }
}
