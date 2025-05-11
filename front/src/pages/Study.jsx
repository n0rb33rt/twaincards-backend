// src/pages/Study.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCardsToLearn } from '../api/learningApi';
import { getCollectionById } from '../api/collectionApi';
import { answerCard } from '../api/learningApi';
import '../styles/study.css';

const CARD_STATES = {
  FRONT: 'front',
  BACK: 'back',
  ANSWERED: 'answered'
};

const Study = () => {
  const { collectionId } = useParams();
  const navigate = useNavigate();

  const [cards, setCards] = useState([]);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [cardState, setCardState] = useState(CARD_STATES.FRONT);
  const [collection, setCollection] = useState(null);
  const [answer, setAnswer] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [studyComplete, setStudyComplete] = useState(false);
  const [stats, setStats] = useState({
    correct: 0,
    incorrect: 0,
    total: 0
  });
  const [startTime, setStartTime] = useState(null);

  useEffect(() => {
    const fetchStudyData = async () => {
      try {
        setIsLoading(true);

        const [collectionData, cardsData] = await Promise.all([
          getCollectionById(collectionId),
          getCardsToLearn(collectionId, 20)
        ]);

        setCollection(collectionData);
        setCards(cardsData);

        if (cardsData.length === 0) {
          setStudyComplete(true);
        }
      } catch (err) {
        console.error('Failed to fetch study data:', err);
        setError('Failed to load study data. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStudyData();
  }, [collectionId]);

  useEffect(() => {
    if (cardState === CARD_STATES.FRONT) {
      setStartTime(Date.now());
    }
  }, [currentCardIndex, cardState]);

  const handleFlip = () => {
    if (cardState === CARD_STATES.FRONT) {
      setCardState(CARD_STATES.BACK);
    }
  };

  const handleAnswer = async (isCorrect) => {
    if (cardState === CARD_STATES.BACK) {
      try {
        setCardState(CARD_STATES.ANSWERED);
        setAnswer(isCorrect);

        const currentCard = cards[currentCardIndex];
        const responseTimeMs = Date.now() - startTime;

        // Update stats
        setStats(prev => ({
          ...prev,
          correct: isCorrect ? prev.correct + 1 : prev.correct,
          incorrect: !isCorrect ? prev.incorrect + 1 : prev.incorrect,
          total: prev.total + 1
        }));

        // Record the answer to the backend
        await answerCard({
          cardId: currentCard.id,
          isCorrect,
          responseTimeMs
        });
      } catch (err) {
        console.error('Failed to record answer:', err);
      }
    }
  };

  const goToNextCard = () => {
    if (currentCardIndex < cards.length - 1) {
      setCurrentCardIndex(currentCardIndex + 1);
      setCardState(CARD_STATES.FRONT);
      setAnswer(null);
    } else {
      setStudyComplete(true);
    }
  };

  if (isLoading) {
    return <div className="loading">Loading study session...</div>;
  }

  if (error) {
    return <div className="error-message">{error}</div>;
  }

  if (studyComplete || cards.length === 0) {
    return (
      <div className="study-complete">
        <h1>Study Session Complete!</h1>

        <div className="study-stats">
          <div className="stat-item">
            <h3>Total Cards Studied</h3>
            <p className="stat-value">{stats.total}</p>
          </div>

          <div className="stat-item correct">
            <h3>Correct Answers</h3>
            <p className="stat-value">{stats.correct}</p>
          </div>

          <div className="stat-item incorrect">
            <h3>Incorrect Answers</h3>
            <p className="stat-value">{stats.incorrect}</p>
          </div>

          <div className="stat-item">
            <h3>Success Rate</h3>
            <p className="stat-value">
              {stats.total > 0 ? Math.round((stats.correct / stats.total) * 100) : 0}%
            </p>
          </div>
        </div>

        <div className="study-actions">
          <button onClick={() => navigate(`/collections/${collectionId}`)} className="collection-btn">
            Back to Collection
          </button>
        </div>
      </div>
    );
  }

  const currentCard = cards[currentCardIndex];

  return (
    <div className="study-container">
      <header className="study-header">
        <h1>Studying: {collection?.name}</h1>
        <div className="study-progress">
          <div className="progress-bar">
            <div
              className="progress-fill"
              style={{ width: `${((currentCardIndex + (cardState === CARD_STATES.ANSWERED ? 1 : 0)) / cards.length) * 100}%` }}
            ></div>
          </div>
          <span className="progress-text">
            {currentCardIndex + 1} of {cards.length} cards
          </span>
        </div>
      </header>

      <div className={`flashcard ${cardState !== CARD_STATES.FRONT ? 'flipped' : ''}`} onClick={handleFlip}>
        <div className="card-side front">
          <div className="card-content">
            <p className="card-language">{collection?.sourceLanguageName}</p>
            <h2 className="card-text">{currentCard?.frontText}</h2>
            <p className="card-hint">Click to reveal answer</p>
          </div>
        </div>

        <div className="card-side back">
          <div className="card-content">
            <p className="card-language">{collection?.targetLanguageName}</p>
            <h2 className="card-text">{currentCard?.backText}</h2>

            {currentCard?.phoneticText && (
              <p className="card-phonetic">[{currentCard.phoneticText}]</p>
            )}

            {currentCard?.exampleUsage && (
              <div className="card-example">
                <h3>Example:</h3>
                <p>{currentCard.exampleUsage}</p>
              </div>
            )}

            {cardState === CARD_STATES.BACK && (
              <div className="answer-buttons">
                <button
                  className="incorrect-btn"
                  onClick={() => handleAnswer(false)}
                >
                  Incorrect
                </button>
                <button
                  className="correct-btn"
                  onClick={() => handleAnswer(true)}
                >
                  Correct
                </button>
              </div>
            )}

            {cardState === CARD_STATES.ANSWERED && (
              <div className="next-button-container">
                <div className={`answer-result ${answer ? 'correct' : 'incorrect'}`}>
                  {answer ? 'Correct!' : 'Incorrect'}
                </div>
                <button
                  className="next-btn"
                  onClick={goToNextCard}
                >
                  Next Card
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="study-info">
        <p><strong>Collection:</strong> {collection?.name}</p>
        <p><strong>From:</strong> {collection?.sourceLanguageName} to {collection?.targetLanguageName}</p>
      </div>
    </div>
  );
};

export default Study;