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
        private Geometry geometry;
        private List<Leg> legs;
        private double distance;
        private double duration;
        private String weight_name;
        private double weight;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Geometry{
            private List<List<Double>> coordinates;
            private String type;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Leg {
            private List<Step> steps;
            private double distance;
            private double duration;
            private String summary;
            private double weight;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Step {
                private List<Intersection> intersections;
                private String driving_side;
                private Geometry geometry;
                private String mode;
                private double duration;
                private Maneuver maneuver;
                private double weight;
                private double distance;
                private String name;

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
                public static class Geometry{
                    private List<List<Double>> coordinates;
                    private String type;
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