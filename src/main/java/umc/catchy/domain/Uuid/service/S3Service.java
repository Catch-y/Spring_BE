package umc.catchy.domain.Uuid.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class S3Service {
    //private final UuidRepository uuidRepository;

    private final AmazonS3Manager s3Manager;

    public String newImage(MultipartFile file){
        String uuid = UUID.randomUUID().toString();
        //Uuid saveUuid = uuidRepository.save(Uuid.builder()
        //        .uuid(uuid).build());

        return s3Manager.uploadFile(uuid, file);
    }

    public String noImage(String imageUrl){
        s3Manager.deleteImage(imageUrl);
        return "delete image";
    }
}
