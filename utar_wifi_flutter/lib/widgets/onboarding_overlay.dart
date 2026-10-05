import 'package:flutter/material.dart';

class OnboardingOverlay extends StatelessWidget {
  final Rect targetRect;
  final String title;
  final String description;
  final int step;
  final int totalSteps;
  final VoidCallback onNext;
  final VoidCallback onSkip;

  const OnboardingOverlay({
    super.key,
    required this.targetRect,
    required this.title,
    required this.description,
    required this.step,
    required this.totalSteps,
    required this.onNext,
    required this.onSkip,
  });

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;
    const dimColor = Color(0xAA000000);

    return Material(
      color: Colors.transparent,
      child: Stack(
        children: [
          // 4-Box Hole architecture for 100% touch passthrough
          if (targetRect != Rect.zero) ...[
            // Top
            Positioned(
              top: 0, left: 0, right: 0,
              height: targetRect.top - 8,
              child: Container(color: dimColor),
            ),
            // Bottom
            Positioned(
              top: targetRect.bottom + 8, left: 0, right: 0, bottom: 0,
              child: Container(color: dimColor),
            ),
            // Left
            Positioned(
              top: targetRect.top - 8, left: 0, width: targetRect.left - 8, height: targetRect.height + 16,
              child: Container(color: dimColor),
            ),
            // Right
            Positioned(
              top: targetRect.top - 8, left: targetRect.right + 8, right: 0, height: targetRect.height + 16,
              child: Container(color: dimColor),
            ),
          ] else
            Container(color: dimColor),

          // Tooltip
          Align(
            alignment: (targetRect == Rect.zero || targetRect.top > screenSize.height / 2) 
              ? Alignment.topCenter : Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 60),
              child: Card(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xFF6750A4))),
                          IconButton(onPressed: onSkip, icon: const Icon(Icons.close, size: 20)),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(description, style: const TextStyle(fontSize: 13)),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text('$step / $totalSteps', style: const TextStyle(fontSize: 11, color: Colors.grey)),
                          ElevatedButton(
                            onPressed: onNext,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFF6750A4),
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                            ),
                            child: const Text('Next'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
