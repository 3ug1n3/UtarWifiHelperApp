class Rc4Cipher {
  static String encrypt(String key, String text) {
    List<int> S = List<int>.generate(256, (i) => i);
    List<int> K = List<int>.generate(256, (i) => key.codeUnitAt(i % key.length));

    int j = 0;
    for (int i = 0; i < 256; i++) {
      j = (j + S[i] + K[i]) % 256;
      int temp = S[i];
      S[i] = S[j];
      S[j] = temp;
    }

    int i = 0;
    j = 0;
    List<int> result = [];
    for (int char in text.codeUnits) {
      i = (i + 1) % 256;
      j = (j + S[i]) % 256;
      int temp = S[i];
      S[i] = S[j];
      S[j] = temp;
      int K = S[(S[i] + S[j]) % 256];
      result.add(char ^ K);
    }

    return result.map((b) => b.toRadixString(16).padLeft(2, '0')).join('');
  }
}
