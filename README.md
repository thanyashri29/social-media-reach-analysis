# Social Media Reach Analysis (Java)

## 1. Problem Statement
Which platforms, content types and posting times give the highest **reach** and **engagement**, and how can a brand grow its audience?

## 2. Dataset
`social_media_data.csv` - 1,000 posts across 5 platforms (Instagram, Facebook, Twitter/X, LinkedIn, YouTube) for 2025.
Columns: post_id, date, platform, content_type, post_hour, followers, hashtags, reach, impressions, likes, comments, shares.
> The data is simulated with realistic patterns, since no real platform data was provided.

## 3. Tools
Java (JDK 17+), Java Collections and Streams for analysis, Java2D (BufferedImage + ImageIO) for charts. No external libraries.

## 4. Data Cleaning
- Removed 10 duplicate rows
- Filled missing likes, comments and hashtags (about 2% each) with the median
- Created: engagement (likes + comments + shares) and engagement rate (engagement / reach x 100)

## 5. Key Findings
| Question | Result |
|---|---|
| Best platform by avg reach | **YouTube (~29,500)**, then Instagram (~26,100) |
| Lowest platform | LinkedIn (~13,600) |
| Best content type | **Reel/Short (~34,200)**, 3.0x the reach of Text posts |
| Best posting window | **6 PM - 9 PM**, +41% reach vs other hours |
| Followers vs reach correlation | 0.63 (moderate) |
| Engagement rate | Similar across platforms (about 6%), so reach is driven mainly by format and timing |

## 6. Visualizations
1. `1_reach_by_platform.png`
2. `2_reach_by_content_type.png`
3. `3_monthly_trend.png`
4. `4_reach_by_hour.png` (peak hours highlighted)
5. `5_reach_by_weekday.png`

## 7. Insights and Recommendations
- Short-form video (Reels/Shorts) gives the highest reach, so make it the core format.
- YouTube and Instagram are the best platforms for awareness.
- Post between 6 PM and 9 PM for a clear reach advantage.
- Follower count helps but does not guarantee reach (correlation 0.63).
- Track engagement rate along with reach.

## 8. Limitations
Simulated data. Hour-wise and weekday-wise differences outside the main evening peak can be random noise, so they are not used for conclusions.

## 9. How to Run
```
javac SocialMediaReachAnalysis.java
java SocialMediaReachAnalysis
```
Output files (CSV and PNG) are created in the same folder.

## 10. Conclusion
Reach depends mainly on content format, platform and posting time, not only audience size. A short-video-first strategy on YouTube and Instagram, posted in the evening, gives the best results.
