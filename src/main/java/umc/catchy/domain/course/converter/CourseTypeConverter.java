package umc.catchy.domain.course.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import umc.catchy.domain.course.domain.CourseType;

@Converter(autoApply = true)
public class CourseTypeConverter implements AttributeConverter<CourseType, String> {

    @Override
    public String convertToDatabaseColumn(CourseType courseType) {
        if (courseType == null) return null;
        return courseType.getValue();
    }

    @Override
    public CourseType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        for (CourseType type : CourseType.values()) {
            if (type.getValue().equals(dbData)) return type;
        }
        throw new IllegalArgumentException("Unknown value: " + dbData);
    }
}
