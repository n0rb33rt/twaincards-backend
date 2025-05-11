// src/pages/study/StudySession.jsx
import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCardsToLearn, answerCard } from '../../api/learningApi';
import { getCollectionById } from '../../api/collectionApi';
import * as studySessionApi from '../../api/studySessionApi';
import '../../styles/study.css';

// Card states
const CARD_STATES = {
  FRONT: 'front',
  BACK: 'back',
  ANSWERED: 'answered',
  TRANSITIONING: 'transitioning' // New state for card transitions
};

const StudySession = () => {
  // Router hooks
  const { collectionId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Parse query parameters
  const queryParams = new URLSearchParams(location.search);
  const startSide = queryParams.get('startSide') || 'front';

  // Card and session state
  const [cards, setCards] = useState([]);
  const [collection, setCollection] = useState(null);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [cardState, setCardState] = useState(CARD_STATES.FRONT);
  const [answer, setAnswer] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sessionData, setSessionData] = useState(null);
  const [isSessionComplete, setIsSessionComplete] = useState(false);
  const [sessionSummary, setSessionSummary] = useState(null);
  
  // Refs
  const cardRef = useRef(null);
  const audioRef = useRef(null);
  const cardStartTime = useRef(Date.now());
  const sessionCreated = useRef(false);
  
  // Session statistics
  const [sessionStats, setSessionStats] = useState({
    startTime: Date.now(),
    timeSpent: 0,
    total: 0,
    correct: 0,
    incorrect: 0
  });

  // Determine the initial card state based on the start side parameter
  const getInitialCardState = (cardIndex) => {
    // Always respect the startSide parameter for all cards
    if (startSide === 'front') {
      return CARD_STATES.FRONT;
    } else if (startSide === 'back') {
      return CARD_STATES.BACK;
    } else if (startSide === 'random') {
      // Randomly choose between front and back
      return Math.random() > 0.5 ? CARD_STATES.FRONT : CARD_STATES.BACK;
    }
    
    // Default to front side for other cases
    return CARD_STATES.FRONT;
  };

  // Load study data when component mounts
  useEffect(() => {
    const fetchStudyData = async () => {
      // Prevent duplicate API calls
      if (sessionCreated.current) return;
      
      try {
        setIsLoading(true);
        setError(null);

        // Fetch collection details and cards to learn in parallel
        const [collectionData, cardsData] = await Promise.all([
          getCollectionById(collectionId),
          getCardsToLearn(collectionId, 20) // Fetch 20 cards to study
        ]);

        setCollection(collectionData);
        
        if (!cardsData || cardsData.length === 0) {
          setIsSessionComplete(true);
          toast.info('No more cards to study in this collection!');
        } else {
          setCards(cardsData);
          
          // Set the initial card state based on the startSide parameter
          setCardState(getInitialCardState(0));
          
          // Create a new study session in the backend
          const deviceInfo = {
            deviceType: navigator.userAgent.includes('Mobile') ? 'mobile' : 'desktop',
            platform: navigator.platform
          };
          
          const createRequest = {
            collectionId: Number(collectionId),
            ...deviceInfo
          };
          
          // Set flag to prevent duplicate API calls
          sessionCreated.current = true;
          
          const sessionResponse = await studySessionApi.createSession(createRequest);
          setSessionData(sessionResponse);
          
          // Reset the session timer
          setSessionStats(prev => ({ ...prev, startTime: Date.now() }));
          // Set the start time for the first card
          cardStartTime.current = Date.now();
        }
      } catch (error) {
        console.error('Error fetching study data:', error);
        
        // Check if this is a daily limit error (403 Forbidden)
        if (error.response?.status === 403) {
          const errorData = error.response?.data;
          // Use the specific error message from the backend
          setError(errorData?.message || 'You have reached your daily study limit');
          toast.error(errorData?.message || 'Daily card limit reached');
        } else {
          setError('Failed to load study data. Please try again.');
          toast.error('Failed to load study data');
        }
        
        // Reset the flag on error so we can retry
        sessionCreated.current = false;
      } finally {
        setIsLoading(false);
      }
    };

    fetchStudyData();
  }, [collectionId, startSide]);

  // Handle card flip
  const handleFlip = () => {
    if (cardState === CARD_STATES.ANSWERED) return;
    
    const newState = cardState === CARD_STATES.FRONT ? CARD_STATES.BACK : CARD_STATES.FRONT;
    setCardState(newState);
    console.log("Card flipped to:", newState);

    // Play audio if available and flipping to back
    if (newState === CARD_STATES.BACK && audioRef.current) {
      audioRef.current.play().catch(err => {
        console.error('Error playing audio:', err);
      });
    }
  };

  // Handle card answer (correct/incorrect)
  const handleAnswer = async (isCorrect) => {
    if (cardState === CARD_STATES.ANSWERED || isSubmitting) return;

    const currentCard = cards[currentCardIndex];
    const responseTimeMs = Date.now() - cardStartTime.current;

    try {
      setIsSubmitting(true);
      setCardState(CARD_STATES.ANSWERED);
      setAnswer(isCorrect);

      // Update session statistics
      setSessionStats(prev => ({
        ...prev,
        correct: isCorrect ? prev.correct + 1 : prev.correct,
        incorrect: !isCorrect ? prev.incorrect + 1 : prev.incorrect,
        total: prev.total + 1
      }));

      // Record answer to the API
      await answerCard({
        cardId: currentCard.id,
        isCorrect,
        responseTimeMs,
        sessionId: sessionData?.id
      });
      
      // Set the card to transitioning state first
      setTimeout(() => {
        setCardState(CARD_STATES.TRANSITIONING);
        
        // Then move to the next card after a brief delay
        setTimeout(() => {
          handleNextCard();
        }, 50);
      }, 950);
      
    } catch (error) {
      console.error('Error recording answer:', error);
      toast.error('Failed to record your answer');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Go to the next card
  const handleNextCard = () => {
    if (currentCardIndex < cards.length - 1) {
      const nextIndex = currentCardIndex + 1;
      setCurrentCardIndex(nextIndex);
      setCardState(getInitialCardState(nextIndex));
      setAnswer(null);

      // Reset the card timer
      cardStartTime.current = Date.now();
    } else {
      completeStudySession();
    }
  };

  // Complete the study session and save results
  const completeStudySession = async () => {
    try {
      // Calculate total time spent
      const totalTimeSpent = Math.floor((Date.now() - sessionStats.startTime) / 1000);
      
      // Include the last card in the total if it was answered
      const totalCards = sessionStats.total + (cardState === CARD_STATES.ANSWERED ? 1 : 0);
      const correctAnswers = answer && cardState === CARD_STATES.ANSWERED 
        ? sessionStats.correct + 1 
        : sessionStats.correct;
      
      // Add debug logging to see what statistics are being sent
      console.log("SESSION STATS DEBUG:", {
        sessionId: sessionData?.id,
        sessionDataExists: !!sessionData,
        currentSessionStats: sessionStats,
        totalCards,
        correctAnswers,
        incorrectAnswers: totalCards - correctAnswers,
        timeSpent: totalTimeSpent
      });
      
      // Save session completion to the backend
      if (sessionData && sessionData.id) {
        const completeRequest = {
          sessionId: sessionData.id,
          cardsReviewed: totalCards,
          correctAnswers: correctAnswers
        };
        
        console.log("Sending to backend:", completeRequest);
        
        const summary = await studySessionApi.completeSession(completeRequest);
        console.log("Received summary from backend:", summary);
        setSessionSummary(summary);
      } else {
        console.error("Cannot complete session: No session ID available");
      }
      
      setSessionStats(prev => ({ 
        ...prev, 
        timeSpent: totalTimeSpent,
        total: totalCards,
        correct: correctAnswers,
        incorrect: totalCards - correctAnswers
      }));
      
      setIsSessionComplete(true);
    } catch (error) {
      console.error('Error completing study session:', error);
      console.error('Error details:', error.response?.data || error.message);
      toast.error('Failed to save session results, but you can continue.');
      
      // Fall back to locally saved session data
      const totalTimeSpent = Math.floor((Date.now() - sessionStats.startTime) / 1000);
      setSessionStats(prev => ({ ...prev, timeSpent: totalTimeSpent }));
      setIsSessionComplete(true);
    }
  };

  // Function to format time in minutes and seconds
  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) {
      return '0m 0s';
    }
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  // Function to retry loading if initial load failed
  const handleRetry = () => {
    window.location.reload();
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Preparing your study session...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <h2>Error Loading Study Session</h2>
        <p>{error}</p>
        <div className="error-actions">
          <button onClick={handleRetry} className="retry-button">
            Retry
          </button>
          <button onClick={() => navigate('/collections')} className="back-button">
            Back to Collections
          </button>
        </div>
      </div>
    );
  }

  // When session is complete, show summary
  if (isSessionComplete) {
    const stats = sessionSummary || {
      cardsStudied: sessionStats.total,
      correctAnswers: sessionStats.correct,
      incorrectAnswers: sessionStats.incorrect,
      successRate: sessionStats.total > 0 ? (sessionStats.correct / sessionStats.total) * 100 : 0,
      timeSpentSeconds: sessionStats.timeSpent
    };

    return (
      <div className="study-complete-container">
        <h1>Study Session Complete!</h1>

        <div className="session-summary-card">
          <div className="stats-grid">
            <div className="stat-item">
              <div className="stat-value">{stats.cardsStudied}</div>
              <div className="stat-label">Cards Studied</div>
            </div>

            <div className="stat-item correct">
              <div className="stat-value">{stats.correctAnswers}</div>
              <div className="stat-label">Correct</div>
            </div>

            <div className="stat-item incorrect">
              <div className="stat-value">{stats.incorrectAnswers}</div>
              <div className="stat-label">Incorrect</div>
            </div>

            <div className="stat-item">
              <div className="stat-value">
                {Math.round(stats.successRate)}%
              </div>
              <div className="stat-label">Success Rate</div>
            </div>
          </div>

          <div className="time-spent">
            <span className="time-icon"></span>
            <span className="time-text">Time spent: {formatTime(stats.timeSpentSeconds)}</span>
          </div>

          <div className="summary-actions">
            <Link to={`/collections/${collectionId}`} className="back-to-collection-btn">
              Back to Collection
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Function to render the current card
  const renderCard = () => {
    if (isLoading || cards.length === 0) return null;
    
    // Don't render anything during transition
    if (cardState === CARD_STATES.TRANSITIONING) {
      return <div className="study-card-container transitioning"></div>;
    }
    
    const currentCard = cards[currentCardIndex];
    const isShowingFront = cardState === CARD_STATES.FRONT;
    
    // Determine which side should show the answer buttons based on startSide
    const showButtonsOnFront = startSide === 'back';
    
    return (
      <div className="study-card-container">
        <div 
          ref={cardRef}
          className={`study-card ${cardState === CARD_STATES.BACK ? 'back' : ''} ${cardState === CARD_STATES.ANSWERED ? 'answered' : ''}`}
          onClick={cardState !== CARD_STATES.ANSWERED ? handleFlip : undefined}
        >
          <div className="card-side-indicator">
            {isShowingFront ? 'Term' : 'Definition'}
          </div>
          
          <div className="card-progress">
            {currentCardIndex + 1} of {cards.length}
          </div>
          
          {/* Front side of the card */}
          <div className="card-front">
            <div className="card-content">
              <div className="card-text">{currentCard.frontText}</div>
              {currentCard.phoneticText && (
                <div className="phonetic-text">{currentCard.phoneticText}</div>
              )}
            </div>
            
            {isShowingFront && !showButtonsOnFront && (
              <div className="flip-indicator">
                Tap to flip card
              </div>
            )}
            
            {/* Show answer buttons on front if startSide is 'back' */}
            {isShowingFront && showButtonsOnFront && (
              <div className="answer-buttons">
                <button 
                  className="incorrect-btn"
                  onClick={(e) => {
                    e.stopPropagation(); // Prevent card flip
                    handleAnswer(false);
                  }}
                  disabled={isSubmitting}
                >
                  Don't Know
                </button>
                <button 
                  className="correct-btn"
                  onClick={(e) => {
                    e.stopPropagation(); // Prevent card flip
                    handleAnswer(true);
                  }}
                  disabled={isSubmitting}
                >
                  Know
                </button>
              </div>
            )}
          </div>
          
          {/* Back side of the card */}
          <div className="card-back">
            <div className="card-content">
              <div className="card-text">{currentCard.backText}</div>
              {currentCard.exampleUsage && (
                <div className="example-usage">
                  <div className="example-label">Example:</div>
                  <div className="example-text">{currentCard.exampleUsage}</div>
                </div>
              )}
              
              {/* Show answer buttons on back only if startSide is not 'back' */}
              {!showButtonsOnFront && (
                <div className="answer-buttons">
                  <button 
                    className="incorrect-btn"
                    onClick={(e) => {
                      e.stopPropagation(); // Prevent card flip
                      handleAnswer(false);
                    }}
                    disabled={isSubmitting}
                  >
                    Don't Know
                  </button>
                  <button 
                    className="correct-btn"
                    onClick={(e) => {
                      e.stopPropagation(); // Prevent card flip
                      handleAnswer(true);
                    }}
                    disabled={isSubmitting}
                  >
                    Know
                  </button>
                </div>
              )}
            </div>
          </div>

          {cardState === CARD_STATES.ANSWERED && (
            <div className={`answer-indicator ${answer ? 'correct' : 'incorrect'}`}>
              {answer ? 'Correct' : 'Incorrect'}
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="study-container">
      <header className="study-header">
        <h1>{collection?.name}</h1>
        <div className="study-progress">
          <div className="progress-bar">
            <div
              className="progress-fill"
              style={{
                width: `${((currentCardIndex + (cardState === CARD_STATES.ANSWERED ? 1 : 0)) / cards.length) * 100}%`
              }}
            ></div>
          </div>
          <div className="progress-text">
            {currentCardIndex + 1} of {cards.length} cards
          </div>
        </div>
      </header>

      <div className="card-container">
        {renderCard()}
      </div>

      <div className="session-info">
        <div className="session-controls">
          <button
            className="end-session-btn"
            onClick={completeStudySession}
          >
            End Session
          </button>
        </div>
      </div>
    </div>
  );
};

export default StudySession;