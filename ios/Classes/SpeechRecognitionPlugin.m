#import "SpeechRecognitionPlugin.h"
#import "speech_recognition_kvc/speech_recognition_kvc-Swift.h"

@implementation SpeechRecognitionPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSpeechRecognitionPlugin registerWithRegistrar:registrar];
}
@end
