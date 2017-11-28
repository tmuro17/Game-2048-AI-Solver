/* 
 * Copyright (C) 2014 Vasilis Vryniotis <bbriniotis at datumbox.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.datumbox.opensource.ai;

import com.datumbox.opensource.dataobjects.Direction;
import com.datumbox.opensource.game.Board;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AIsolver class that uses Artificial Intelligence to estimate the next move.
 *
 * @author Vasilis Vryniotis <bbriniotis at datumbox.com>
 */
public class AIsolver
{

	/**
	 * Method that finds the best next move.
	 *
	 * @param theBoard The board on which to work
	 * @param depth The depth to look
	 *
	 * @return The <code>Direction</code> of the best move
	 *
	 * @throws CloneNotSupportedException Thrown if there is an error with cloning
	 */
	public static Direction findBestMove(Board theBoard, int depth) throws CloneNotSupportedException
	{
		//Map<String, Object> result = minimax(theBoard, depth, Player.USER);

		Map<String, Object> result = alphabeta(theBoard, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, Player.USER);

		return (Direction) result.get("Direction");
	}

	/**
	 * Finds the best move by using the Minimax algorithm.
	 *
	 * @param theBoard The board on which to work
	 * @param depth The depth to look
	 * @param player Which player is making the move
	 *
	 * @return A map representing the best moves
	 *
	 * @throws CloneNotSupportedException Thrown if there is an error with cloning
	 */
	@SuppressWarnings("unused") private static Map<String, Object> minimax(Board theBoard, int depth, Player player) throws CloneNotSupportedException
	{
		Map<String, Object> result = new HashMap<>();

		Direction bestDirection = null;
		int bestScore;

		if(depth == 0 || theBoard.isGameTerminated())
			bestScore = heuristicScore(theBoard.getScore(), theBoard.getNumberOfEmptyCells(),
			                           calculateClusteringScore(theBoard.getBoardArray()));
		else if(player == Player.USER)
		{
			bestScore = Integer.MIN_VALUE;

			for(Direction direction : Direction.values())
			{
				Board newBoard = (Board) theBoard.clone();

				int points = newBoard.move(direction);

				if(points == 0 && newBoard.isEqual(theBoard.getBoardArray(), newBoard.getBoardArray()))
					continue;

				Map<String, Object> currentResult = minimax(newBoard, depth - 1, Player.COMPUTER);
				int currentScore = ((Number) currentResult.get("Score")).intValue();
				if(currentScore > bestScore)
				{ //maximize score
					bestScore = currentScore;
					bestDirection = direction;
				}
			}
		}
		else
		{
			bestScore = Integer.MAX_VALUE;

			List<Integer> moves = theBoard.getEmptyCellIds();

			if(moves.isEmpty())
				bestScore = 0;

			int[] possibleValues = {2, 4};

			int i, j;

			for(Integer cellId : moves)
			{
				i = cellId / Board.BOARD_SIZE;
				j = cellId % Board.BOARD_SIZE;

				for(int value : possibleValues)
				{
					Board newBoard = (Board) theBoard.clone();
					newBoard.setEmptyCell(i, j, value);

					Map<String, Object> currentResult = minimax(newBoard, depth - 1, Player.USER);
					int currentScore = ((Number) currentResult.get("Score")).intValue();

					if(currentScore < bestScore)
						bestScore = currentScore; //minimize best score
				}
			}
		}

		result.put("Score", bestScore);
		result.put("Direction", bestDirection);

		return result;
	}

	/**
	 * Finds the best move bay using the Alpha-Beta pruning algorithm.
	 *
	 * @param theBoard The board on which to look
	 * @param depth The depth to look
	 * @param alpha The alpha for the alpha beta
	 * @param beta The beta for the alpha beta
	 * @param player The player who is making the move
	 *
	 * @return A map representing the best moves
	 *
	 * @throws CloneNotSupportedException Thrown if there is an error with cloning
	 */
	private static Map<String, Object> alphabeta(Board theBoard, int depth, int alpha, int beta, Player player)
			throws CloneNotSupportedException
	{
		Map<String, Object> result = new HashMap<>();

		Direction bestDirection = null;
		int bestScore;

		if(theBoard.isGameTerminated())
			bestScore = theBoard.hasWon() ? Integer.MAX_VALUE : Math.min(theBoard.getScore(), 1);
		else if(depth == 0)
			bestScore = heuristicScore(theBoard.getScore(), theBoard.getNumberOfEmptyCells(),
			                           calculateClusteringScore(theBoard.getBoardArray()));
		else if(player == Player.USER)
		{
			for(Direction direction : Direction.values())
			{
				Board newBoard = (Board) theBoard.clone();

				int points = newBoard.move(direction);

				if(points == 0 && newBoard.isEqual(theBoard.getBoardArray(), newBoard.getBoardArray()))
					continue;

				Map<String, Object> currentResult = alphabeta(newBoard, depth - 1, alpha, beta, Player.COMPUTER);
				int currentScore = ((Number) currentResult.get("Score")).intValue();

				if(currentScore > alpha)
				{ //maximize score
					alpha = currentScore;
					bestDirection = direction;
				}

				if(beta <= alpha)
					break; //beta cutoff
			}

			bestScore = alpha;
		}
		else
		{
			List<Integer> moves = theBoard.getEmptyCellIds();
			int[] possibleValues = {2, 4};

			int i, j;
			abloop:
			for(Integer cellId : moves)
			{
				i = cellId / Board.BOARD_SIZE;
				j = cellId % Board.BOARD_SIZE;

				for(int value : possibleValues)
				{
					Board newBoard = (Board) theBoard.clone();
					newBoard.setEmptyCell(i, j, value);

					Map<String, Object> currentResult = alphabeta(newBoard, depth - 1, alpha, beta, Player.USER);
					int currentScore = ((Number) currentResult.get("Score")).intValue();

					if(currentScore < beta)
						beta = currentScore; //minimize best score

					if(beta <= alpha)
						break abloop; //alpha cutoff
				}
			}

			bestScore = beta;

			if(moves.isEmpty())
				bestScore = 0;
		}

		result.put("Score", bestScore);
		result.put("Direction", bestDirection);

		return result;
	}

	/**
	 * Estimates a heuristic score by taking into account the real score, the
	 * number of empty cells and the clustering score of the board.
	 *
	 * @param actualScore The actual score of the game
	 * @param numberOfEmptyCells The number of empty cells that exist
	 * @param clusteringScore The clustering score of the board
	 *
	 * @return The heuristically calculated score
	 */
	private static int heuristicScore(int actualScore, int numberOfEmptyCells, int clusteringScore)
	{
		int score = (int) (actualScore + Math.log(actualScore) * numberOfEmptyCells - clusteringScore);
		return Math.max(score, Math.min(actualScore, 1));
	}

	/**
	 * Calculates a heuristic variance-like score that measures how clustered the
	 * board is.
	 *
	 * @param boardArray An array that represents the board
	 *
	 * @return The clustering score of the board representation
	 */
	private static int calculateClusteringScore(int[][] boardArray)
	{
		int clusteringScore = 0;

		int[] neighbors = {-1, 0, 1};

		for(int i = 0; i < boardArray.length; ++i)
		{
			for(int j = 0; j < boardArray.length; ++j)
			{
				if(boardArray[i][j] == 0)
					continue; //ignore empty cells

				//clusteringScore-=boardArray[i][j];

				//for every pixel find the distance from each neighbors
				int numOfNeighbors = 0;
				int sum = 0;
				for(int k : neighbors)
				{
					int x = i + k;
					if(x < 0 || x >= boardArray.length)
						continue;
					for(int l : neighbors)
					{
						int y = j + l;
						if(y < 0 || y >= boardArray.length)
							continue;

						if(boardArray[x][y] > 0)
						{
							++numOfNeighbors;
							sum += Math.abs(boardArray[i][j] - boardArray[x][y]);
						}
					}
				}

				clusteringScore += sum / numOfNeighbors;
			}
		}

		return clusteringScore;
	}

	/**
	 * Player vs Computer enum class
	 */
	public enum Player
	{
		/**
		 * Computer
		 */
		COMPUTER,

		/**
		 * User
		 */
		USER
	}

}
