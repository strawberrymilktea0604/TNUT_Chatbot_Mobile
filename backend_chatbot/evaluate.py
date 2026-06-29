import pandas as pd
from bert_score import score

# To run this script, you need to install bert-score and a backend library like torch:
# pip install bert-score torch

def evaluate_csv(file_path):
    """
    Evaluates the chatbot's performance based on a CSV file using BERTScore.

    The CSV file should have three columns:
    1. question
    2. expected_answer
    3. actual_answer
    """
    try:
        df = pd.read_csv(file_path)
    except FileNotFoundError:
        print(f"Error: The file at {file_path} was not found.")
        return

    # Ensure the required columns exist
    required_columns = ['question', 'expected_answer', 'actual_answer']
    if not all(col in df.columns for col in required_columns):
        print(f"Error: The CSV file must contain the following columns: {', '.join(required_columns)}")
        return

    # Remove rows with missing values in expected or actual answers
    df.dropna(subset=['expected_answer', 'actual_answer'], inplace=True)

    expected_answers = df['expected_answer'].tolist()
    actual_answers = df['actual_answer'].tolist()

    if not expected_answers or not actual_answers:
        print("Error: No valid data to evaluate after cleaning.")
        return

    # Calculate BERTScore
    # lang="vi" for Vietnamese. It will use a multilingual model.
    P, R, F1 = score(actual_answers, expected_answers, lang="vi", verbose=True)

    # Add scores to the DataFrame for detailed review
    df['precision'] = P.numpy()
    df['recall'] = R.numpy()
    df['f1_score'] = F1.numpy()

    print("\n--- Evaluation Results ---")
    print(f"Total questions evaluated: {len(df)}")
    print(f"Average Precision: {P.mean():.4f}")
    print(f"Average Recall:    {R.mean():.4f}")
    print(f"Average F1 Score:  {F1.mean():.4f}")
    print("--------------------------\n")

    # Display detailed scores for each question
    print("Detailed scores per question:")
    print(df[['question', 'f1_score', 'precision', 'recall']].to_string())

    # Save the detailed results to a new CSV file
    output_path = 'evaluation_results.csv'
    df.to_csv(output_path, index=False, encoding='utf-8-sig')
    print(f"\nDetailed results saved to {output_path}")


import os

if __name__ == '__main__':
    csv_path = 'evaluation_data.csv'

    # Check if the evaluation CSV file exists. If not, create it.
    if not os.path.exists(csv_path):
        print(f"'{csv_path}' not found. Creating a new sample file.")
        questions = [
            "E-Learning là gì? Những ưu điểm của phương thức đạo tạo E-learning?",
            "Hình thức học này có điểm danh hay yêu cầu phải học trong 1 khung giờ nhất định không?",
            "Em muốn hỏi về địa điểm thi kết thúc học phần ở đâu?",
            "Việc thi hết môn/kết thúc học phần nếu trường hợp không thể tham gia thi có được xin hoãn thi không?",
            "Nếu em không qua môn, em sẽ phải học lại như thế nào?",
            "Quy định về xét tốt nghiệp của Nhà trường như thế nào?",
            "Muốn học nhanh thì có rút ngắn thời gian đào tạo được không?",
            "Quy định về các học phần thực hành, thí nghiệm như thế nào?",
            "Cách thức đăng ký học phần như thế nào?",
            "Em có thể chuyển ngành hoặc học cùng lúc hai chương trình không?",
            "Em muốn hỏi về thủ tục xét miễn môn như thế nào?",
            "Làm thế nào để cân bằng giữa việc học và công việc cá nhân?",
            "Điểm trung bình phải bao nhiêu mới loại giỏi",
            "Mô tả cho tôi về học phần Triết học Mác Lê - nin",
            "Fanpage của nhà trường là gì"
        ]
        
        data = {
            'question': questions,
            'expected_answer': ['Please fill in the expected answer.'] * len(questions),
            'actual_answer': ['Please fill in the actual answer from the chatbot.'] * len(questions)
        }
        
        sample_df = pd.DataFrame(data)
        sample_df.to_csv(csv_path, index=False, encoding='utf-8-sig')
        print(f"A sample file has been created at '{csv_path}'.")
        print("Please fill it with your data and run the script again.")
    else:
        # If the file exists, proceed with the evaluation.
        print(f"Found '{csv_path}'. Proceeding with evaluation.")
        evaluate_csv(csv_path)