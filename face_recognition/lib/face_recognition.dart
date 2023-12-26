
import 'face_recognition_platform_interface.dart';

class FaceRecognition {
  Future<String?> getPlatformVersion() {
    return FaceRecognitionPlatform.instance.getPlatformVersion();
  }
  Future<dynamic> addFace({required String name}) {
    return FaceRecognitionPlatform.instance.addFace(name: name);
  }
}
