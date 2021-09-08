import 'package:flutter/material.dart';
import 'package:scan_preview/scan_preview_widget.dart';

class QRScanner extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: double.infinity,
      child: ScanPreviewWidget(
        onScanResult: (result) {
          Navigator.pop(context, result);
        },
      ),
    );
  }
}