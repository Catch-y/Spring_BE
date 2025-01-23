package umc.catchy.infra.osrm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OsrmResponse {
    private String code;
    private List<Route> routes;
    private List<Waypoint> waypoints;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Route {
        private String geometry;
        private List<Leg> legs;
        private double weight;
        private double duration;
        private double distance;
        private String weight_name;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Leg {
            private List<Step> steps;
            private String summary;
            private double weight;
            private double duration;
            private double distance;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Step {
                private String geometry;
                private Maneuver maneuver;
                private String mode;
                private String driving_side;
                private String name;
                private List<Intersection> intersections;
                private double weight;
                private double duration;
                private double distance;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                public static class Maneuver {
                    private int bearing_after;
                    private int bearing_before;
                    private List<Double> location;
                    private String type;
                    private String modifier;
                }

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                public static class Intersection {
                    private int out;
                    private int in;
                    private List<Boolean> entry;
                    private List<Integer> bearings;
                    private List<Double> location;
                    private List<Lane> lanes;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    public static class Lane {
                        private boolean valid;
                        private List<String> indications;
                    }
                }
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Waypoint {
        private String hint;
        private double distance;
        private String name;
        private List<Double> location;
    }
}