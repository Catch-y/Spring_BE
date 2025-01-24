package umc.catchy.infra.aws.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.global.config.s3.AmazonConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager {

    private final AmazonS3 s3;

    private final AmazonConfig amazonConfig;

    //private final UuidRepository uuidRepository;

    public String uploadFile(String keyName, MultipartFile file){
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        try{
            s3.putObject(new PutObjectRequest(amazonConfig.getBucket(), keyName, file.getInputStream(), metadata));
        }catch (IOException e){
            log.error("error at AmazonS3Manager uploadFile : {}",(Object) e.getStackTrace());
        }
        return s3.getUrl(amazonConfig.getBucket(), keyName).toString();
    }

    public void deleteImage(String fileUrl){
        try{
            String splitStr = ".com/";
            String fileName = fileUrl.substring(fileUrl.lastIndexOf(splitStr) + splitStr.length());
            s3.deleteObject(new DeleteObjectRequest(amazonConfig.getBucket(), fileName));
        }
        catch(SdkClientException e){
            log.error("Error deleting file from s3");
        }
    }

    public String uploadInputStream(String keyName, InputStream inputStream, String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);

        try {
            // InputStream 복사
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // InputStream을 다시 사용
            InputStream validatedInputStream = new ByteArrayInputStream(outputStream.toByteArray());
            long validatedContentLength = outputStream.size();

            // 검증된 InputStream으로 업로드
            metadata.setContentLength(validatedContentLength);
            s3.putObject(new PutObjectRequest(amazonConfig.getBucket(), keyName, validatedInputStream, metadata));
            System.out.println("Uploaded content length: " + validatedContentLength);
        } catch (Exception e) {
            log.error("Error uploading InputStream to S3: {}", e.getMessage());
            throw new RuntimeException("Error uploading InputStream to S3", e);
        }

        return getFileUrl(keyName);
    }

    public String getFileUrl(String keyName) {
        return s3.getUrl(amazonConfig.getBucket(), keyName).toString();
    }
}
