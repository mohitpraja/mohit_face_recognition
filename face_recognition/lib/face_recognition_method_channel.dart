import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'face_recognition_platform_interface.dart';

/// An implementation of [FaceRecognitionPlatform] that uses method channels.
class MethodChannelFaceRecognition extends FaceRecognitionPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('face_recognition');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<dynamic> addFace({required String name}) async {
    final result = await methodChannel.invokeMethod<String>('addFace',{'faceData': name});
    return result;
  }
}
