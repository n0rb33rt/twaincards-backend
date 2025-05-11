// src/pages/Statistics.jsx
import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import {
  getUserStatistics,
  getActivityStatistics,
  getSummaryStatistics,
  getTopUsersByLearnedCards,
  getTopUsers
} from '../api/statisticsApi';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  RadialLinearScale,
  Filler
} from 'chart.js';
import { Doughnut, Bar, Line, Radar } from 'react-chartjs-2';
import { format, subDays, parseISO } from 'date-fns';
import '../styles/main.css';
import axios from 'axios';

// Register ChartJS components
ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  RadialLinearScale,
  Filler
);

const Statistics = () => {
  const [userStats, setUserStats] = useState(null);
  const [activityStats, setActivityStats] = useState(null);
  const [summaryStats, setSummaryStats] = useState(null);
  const [timeRange, setTimeRange] = useState(30); // Default to 30 days
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewMode, setViewMode] = useState('summary'); // 'summary', 'activity', 'leaderboard'
  const [topLearnedCards, setTopLearnedCards] = useState([]);
  const [topLearningStreak, setTopLearningStreak] = useState([]);

  useEffect(() => {
    const fetchStatisticsData = async () => {
      setIsLoading(true);
      setError(null);

      try {
        if (viewMode === 'leaderboard') {
          // Get top users by learned cards for both leaderboards
          // since the learning streak endpoint no longer exists
          const topUsers = await getTopUsersByLearnedCards(10);
          
          setTopLearnedCards(topUsers);
          setTopLearningStreak(topUsers); // Use the same data for both leaderboards
          setIsLoading(false);
          return;
        }

        const [user, activity, summary] = await Promise.all([
          getUserStatistics(),
          getActivityStatistics(timeRange),
          getSummaryStatistics(timeRange)
        ]);

        setUserStats(user);
        setActivityStats(activity);
        setSummaryStats(summary);
      } catch (err) {
        console.error('Error fetching statistics:', err);
        setError('Failed to load statistics. Please try again later.');
        toast.error('Failed to load statistics. Please try again later.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStatisticsData();
  }, [timeRange, viewMode]);

  const handleTimeRangeChange = (days) => {
    setTimeRange(days);
  };

  // Prepare data for progress chart (learned vs total)
  const progressChartData = userStats ? {
    labels: [
      'Learned Cards',
      'In Progress Cards',
      'To Learn Cards'
    ],
    datasets: [
      {
        data: [
          userStats.learnedCards || 0,
          userStats.cardsInProgress || 0, 
          userStats.cardsToLearn || 0
        ],
        backgroundColor: [
          'rgba(40, 167, 69, 0.7)',  // green for learned
          'rgba(255, 193, 7, 0.7)',   // yellow for in progress
          'rgba(108, 117, 125, 0.7)'  // grey for to learn
        ],
        borderColor: [
          'rgba(40, 167, 69, 1)',
          'rgba(255, 193, 7, 1)',
          'rgba(108, 117, 125, 1)'
        ],
        borderWidth: 1
      }
    ]
  } : null;

  // Prepare data for activity heatmap
  const getActivityHeatmapData = () => {
    if (!activityStats || !activityStats.dailyActivity) return [];

    // Get most recent days first (up to timeRange days)
    const recentActivity = activityStats.dailyActivity
      .slice(0, timeRange)
      .reverse(); // To have most recent days first

    return recentActivity.map(day => {
      // Convert [2025, 5, 5] to a proper date string
      const dateArr = day.date;
      // Format date labels
      const formattedDate = `${dateArr[0]}-${String(dateArr[1]).padStart(2, '0')}-${String(dateArr[2]).padStart(2, '0')}`;
      
      return {
        date: formattedDate,
        cardsStudied: day.cardsStudied,
        newCardsLearned: day.newCardsLearned
      };
    });
  };

  // Prepare data for activity timeline
  const getActivityTimelineData = () => {
    if (!activityStats || !activityStats.dailyActivity) return null;

    // Get the daily activity data and reverse for chronological order
    const dailyData = [...activityStats.dailyActivity].reverse();
    
    // Filter to remove days with zero activity if showing a summary view
    const filteredData = viewMode === 'summary' 
      ? dailyData.filter(day => day.cardsStudied > 0).slice(0, 14)
      : dailyData.slice(0, timeRange);
    
    // Format the dates and prepare the dataset
    const labels = filteredData.map(day => {
      const [year, month, dayOfMonth] = day.date;
      return format(new Date(year, month - 1, dayOfMonth), 'MMM d');
    });
    
    return {
      labels,
      datasets: [
        {
          label: 'Cards Studied',
          data: filteredData.map(day => day.cardsStudied),
          borderColor: 'rgba(84, 101, 255, 1)',
          backgroundColor: 'rgba(84, 101, 255, 0.2)',
          fill: true,
          tension: 0.4
        },
      ]
    };
  };

  // Format date array from API [year, month, day] to readable string
  const formatDateArray = (dateArray) => {
    if (!dateArray || dateArray.length !== 3) return '';
    const date = new Date(dateArray[0], dateArray[1] - 1, dateArray[2]);
    return format(date, 'MMM dd');
  };

  // Prepare data for activity chart (last 30 days)
  const prepareActivityChartData = () => {
    if (!activityStats || !activityStats.dailyActivity) return null;

    // Get the last 30 days of activity in the correct order (most recent last)
    const last30Days = activityStats.dailyActivity.slice(0, 30).reverse();

    return {
      labels: last30Days.map(day => formatDateArray(day.date)),
      datasets: [
        {
          label: 'Cards Studied',
          data: last30Days.map(day => day.cardsStudied),
          fill: true,
          backgroundColor: 'rgba(78, 115, 223, 0.1)',
          borderColor: 'rgba(78, 115, 223, 1)',
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: 'rgba(78, 115, 223, 1)',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
        },
      ]
    };
  };

  // Prepare data for success rate doughnut chart
  const prepareSuccessRateChartData = () => {
    if (!summaryStats) return null;
    
    return {
      labels: ['Correct', 'Incorrect'],
      datasets: [
        {
          data: [summaryStats.totalCorrectAnswers, summaryStats.totalIncorrectAnswers],
          backgroundColor: ['rgba(40, 167, 69, 0.8)', 'rgba(220, 53, 69, 0.8)'],
          borderColor: ['rgba(40, 167, 69, 1)', 'rgba(220, 53, 69, 1)'],
          borderWidth: 1,
        }
      ]
    };
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading your statistics...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <h2>Error Loading Statistics</h2>
        <p>{error}</p>
        <button onClick={() => window.location.reload()} className="retry-button">
          Retry
        </button>
      </div>
    );
  }

  // Check if the user has any statistics
  const hasData = userStats && userStats.totalCards > 0;

  // Skip the empty state check for leaderboard view
  if (!hasData && viewMode !== 'leaderboard') {
    return (
      <div className="statistics-empty-state">
        <h2>No Statistics Available</h2>
        <p>Start studying cards to generate statistics and track your progress.</p>
        <a href="/study" className="start-studying-btn">
          Start Studying
        </a>
      </div>
    );
  }

  const activityData = getActivityHeatmapData();
  const activityTimelineData = getActivityTimelineData();

  return (
    <div className="statistics-container">
      <header className="statistics-header">
        <div className="header-content">
          <h1>Learning Statistics</h1>
          <p className="statistics-subtitle">
            Track your progress and analyze your learning patterns
          </p>
        </div>
        
        <div className="view-controls">
          <div className="view-mode-selector">
            <button 
              className={viewMode === 'summary' ? 'active' : ''} 
              onClick={() => setViewMode('summary')}
            >
              Summary
            </button>
            <button 
              className={viewMode === 'activity' ? 'active' : ''} 
              onClick={() => setViewMode('activity')}
            >
              Activity
            </button>
            <button 
              className={viewMode === 'leaderboard' ? 'active' : ''} 
              onClick={() => setViewMode('leaderboard')}
            >
              Leaderboard
            </button>
          </div>
          
          {viewMode !== 'leaderboard' && (
            <div className="time-range-selector">
              <span className="selector-label">Time range:</span>
              <div className="selector-buttons">
                <button
                  className={timeRange === 7 ? 'active' : ''}
                  onClick={() => handleTimeRangeChange(7)}
                >
                  7 days
                </button>
                <button
                  className={timeRange === 30 ? 'active' : ''}
                  onClick={() => handleTimeRangeChange(30)}
                >
                  30 days
                </button>
                <button
                  className={timeRange === 90 ? 'active' : ''}
                  onClick={() => handleTimeRangeChange(90)}
                >
                  90 days
                </button>
              </div>
            </div>
          )}
        </div>
      </header>

      {/* Main Statistics Dashboard */}
      {viewMode === 'summary' && (
        <div className="statistics-dashboard">
          {/* Key Metrics */}
          <div className="metrics-row">
            <div className="metric-card">
              <div className="metric-icon total-icon">📚</div>
              <div className="metric-content">
                <h3>Total Cards</h3>
                <p className="metric-value">{userStats?.totalCards || 0}</p>
              </div>
            </div>
            
            <div className="metric-card">
              <div className="metric-icon learned-icon">✅</div>
              <div className="metric-content">
                <h3>Learned Cards</h3>
                <p className="metric-value">{userStats?.learnedCards || 0}</p>
              </div>
            </div>
            
            <div className="metric-card">
              <div className="metric-icon progress-icon">🔄</div>
              <div className="metric-content">
                <h3>In Progress</h3>
                <p className="metric-value">{userStats?.cardsInProgress || 0}</p>
              </div>
            </div>
            
            <div className="metric-card">
              <div className="metric-icon streak-icon">🔥</div>
              <div className="metric-content">
                <h3>Current Streak</h3>
                <p className="metric-value">{userStats?.learningStreakDays || 0} days</p>
              </div>
            </div>
            
            <div className="metric-card">
              <div className="metric-icon completion-icon">🎯</div>
              <div className="metric-content">
                <h3>Completion</h3>
                <p className="metric-value">
                  {userStats?.completionPercentage?.toFixed(2) || 0}%
                </p>
              </div>
            </div>
          </div>

          {/* Summary Statistics */}
          <div className="summary-section">
            <h2>Last {timeRange} Days Summary</h2>
            <div className="summary-row">
              <div className="summary-card">
                <div className="summary-icon">👀</div>
                <div className="summary-content">
                  <h3>Total Reviews</h3>
                  <p className="summary-value">{summaryStats?.totalReviews || 0}</p>
                </div>
              </div>
              
              <div className="summary-card">
                <div className="summary-icon">🎯</div>
                <div className="summary-content">
                  <h3>Success Rate</h3>
                  <p className="summary-value">{summaryStats?.successRate || 0}%</p>
                </div>
              </div>
              
              <div className="summary-card">
                <div className="summary-icon">🧠</div>
                <div className="summary-content">
                  <h3>Unique Cards</h3>
                  <p className="summary-value">{summaryStats?.uniqueCardsStudied || 0}</p>
                </div>
              </div>
              
              <div className="summary-card">
                <div className="summary-icon">📝</div>
                <div className="summary-content">
                  <h3>Study Sessions</h3>
                  <p className="summary-value">{summaryStats?.studySessionsCount || 0}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Progress Visualization */}
          <div className="visualization-row">
            <div className="chart-card">
              <h2>Card Progress</h2>
              <div className="chart-container-donut">
                {progressChartData && (
                  <Doughnut
                    data={progressChartData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: {
                          position: 'bottom',
                          labels: {
                            boxWidth: 12,
                            padding: 15,
                            font: {
                              size: 12
                            }
                          }
                        },
                        tooltip: {
                          callbacks: {
                            label: function(context) {
                              const label = context.label || '';
                              const value = context.parsed || 0;
                              const total = context.dataset.data.reduce((a, b) => a + b, 0);
                              const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                              return `${label}: ${value} (${percentage}%)`;
                            }
                          }
                        }
                      },
                      cutout: '70%'
                    }}
                  />
                )}
              </div>
            </div>
            
            <div className="chart-card">
              <h2>Study Activity</h2>
              <div className="chart-container-line">
                {activityTimelineData && (
                  <Line
                    data={activityTimelineData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: {
                          position: 'bottom',
                          labels: {
                            boxWidth: 12,
                            padding: 15,
                            font: {
                              size: 12
                            }
                          }
                        }
                      },
                      scales: {
                        y: {
                          beginAtZero: true,
                          ticks: {
                            precision: 0
                          }
                        }
                      }
                    }}
                  />
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Activity View */}
      {viewMode === 'activity' && (
        <div className="activity-dashboard">
          <div className="metrics-row">
            <div className="metric-card">
              <div className="metric-icon">📅</div>
              <div className="metric-content">
                <h3>Days Active</h3>
                <p className="metric-value">{activityStats?.totalDaysActive || 0}</p>
              </div>
            </div>
            
            <div className="metric-card">
              <div className="metric-icon">🔥</div>
              <div className="metric-content">
                <h3>Current Streak</h3>
                <p className="metric-value">{activityStats?.currentStreakDays || 0} days</p>
              </div>
            </div>
            
            <div className="metric-card">
              <div className="metric-icon">📊</div>
              <div className="metric-content">
                <h3>Avg. Cards/Day</h3>
                <p className="metric-value">{activityStats?.averageCardsPerDay || 0}</p>
              </div>
            </div>
          </div>

          {/* Daily Activity Timeline */}
          <div className="chart-card full-width">
            <h2>Daily Activity</h2>
            <div className="chart-container-line large">
              {activityTimelineData && (
                <Line
                  data={activityTimelineData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                      legend: {
                        position: 'bottom',
                        labels: {
                          boxWidth: 12,
                          padding: 15,
                          font: {
                            size: 12
                          }
                        }
                      }
                    },
                    scales: {
                      y: {
                        beginAtZero: true,
                        ticks: {
                          precision: 0
                        }
                      }
                    }
                  }}
                />
              )}
            </div>
          </div>
        </div>
      )}

      {/* Leaderboard View */}
      {viewMode === 'leaderboard' && (
        <div className="leaderboard-dashboard">
          <div className="leaderboard-title">
            <h2>Global Leaderboards</h2>
            <p>See how you compare with other learners</p>
          </div>
          
          <div className="leaderboard-row">
            {/* Top Learned Cards Leaderboard */}
            <div className="leaderboard-card">
              <h3>Top Learners by Cards Mastered</h3>
              {topLearnedCards && topLearnedCards.length > 0 ? (
                <div className="leaderboard-table-container">
                  <table className="leaderboard-table">
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>User</th>
                        <th>Cards Learned</th>
                      </tr>
                    </thead>
                    <tbody>
                      {topLearnedCards.map((user, index) => (
                        <tr key={`learned-${user.id}`} className={index < 3 ? `rank-${index + 1}` : ''}>
                          <td>{index + 1}</td>
                          <td>{user.username || 'Anonymous'}</td>
                          <td>{user.learnedCards}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="no-data-message">No leaderboard data available yet.</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Statistics;