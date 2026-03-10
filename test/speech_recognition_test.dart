import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:speech_recognition_kvc/speech_recognition_kvc.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SpeechRecognition speechRecognition;
  final List<MethodCall> log = <MethodCall>[];

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('speech_recognition'),
      (MethodCall methodCall) async {
        log.add(methodCall);
        if (methodCall.method == 'speech.activate') {
          return true;
        } else if (methodCall.method == 'speech.listen') {
          return true;
        }
        return null;
      },
    );
    speechRecognition = SpeechRecognition();
    log.clear();
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('speech_recognition'),
      null,
    );
  });

  test('activate invokes correct method', () async {
    final result = await speechRecognition.activate();
    expect(result, isTrue);
    expect(log, hasLength(1));
    expect(log.first.method, 'speech.activate');
  });

  test('listen invokes correct method with arguments', () async {
    await speechRecognition.listen(locale: 'fr_FR');
    expect(log, hasLength(1));
    expect(log.first.method, 'speech.listen');
    expect(log.first.arguments, 'fr_FR');
  });

  test('cancel invokes correct method', () async {
    await speechRecognition.cancel();
    expect(log, hasLength(1));
    expect(log.first.method, 'speech.cancel');
  });

  test('stop invokes correct method', () async {
    await speechRecognition.stop();
    expect(log, hasLength(1));
    expect(log.first.method, 'speech.stop');
  });

  test('availabilityHandler is called on speech.onSpeechAvailability', () async {
    bool? availability;
    speechRecognition.setAvailabilityHandler((result) {
      availability = result;
    });

    await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .handlePlatformMessage(
      'speech_recognition',
      const StandardMethodCodec().encodeMethodCall(
        const MethodCall('speech.onSpeechAvailability', true),
      ),
      (ByteData? data) {},
    );

    expect(availability, isTrue);
  });

  test('recognitionResultHandler is called on speech.onSpeech', () async {
    String? resultText;
    speechRecognition.setRecognitionResultHandler((text) {
      resultText = text;
    });

    await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .handlePlatformMessage(
      'speech_recognition',
      const StandardMethodCodec().encodeMethodCall(
        const MethodCall('speech.onSpeech', 'hello world'),
      ),
      (ByteData? data) {},
    );

    expect(resultText, 'hello world');
  });

  test('errorHandler is called on speech.onError', () async {
    SpeechRecognitionError? errorResult;
    speechRecognition.setErrorHandler((error) {
      errorResult = error;
    });

    await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .handlePlatformMessage(
      'speech_recognition',
      const StandardMethodCodec().encodeMethodCall(
        const MethodCall('speech.onError', 9), // 9 is noPermission
      ),
      (ByteData? data) {},
    );

    expect(errorResult, SpeechRecognitionError.noPermission);
  });
}
